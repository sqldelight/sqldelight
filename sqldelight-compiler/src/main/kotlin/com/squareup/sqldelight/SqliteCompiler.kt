/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import com.squareup.sqldelight.model.body
import com.squareup.sqldelight.model.constantName
import com.squareup.sqldelight.model.identifier
import com.squareup.sqldelight.model.isNullable
import com.squareup.sqldelight.model.javaType
import com.squareup.sqldelight.model.methodName
import com.squareup.sqldelight.model.name
import com.squareup.sqldelight.model.sqliteText
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream
import java.util.Locale.US
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

class SqliteCompiler {
  fun write(
      parseContext: SqliteParser.ParseContext,
      fileName: String,
      relativePath: String,
      projectPath: String
  ): Status {
    try {
      val packageName = relativePath.split(File.separatorChar).dropLast(1).joinToString(".")
      val className = interfaceName(fileName)
      val typeSpec = TypeSpec.interfaceBuilder(className)
          .addModifiers(PUBLIC)

      if (parseContext.sql_stmt_list().create_table_stmt() != null) {
        val table = parseContext.sql_stmt_list().create_table_stmt()
        typeSpec.addField(FieldSpec.builder(String::class.java, TABLE_NAME)
            .addModifiers(PUBLIC, STATIC, FINAL)
            .initializer("\$S", table.table_name().text)
            .build())

        for (column in table.column_def()) {
          if (column.constantName == TABLE_NAME) {
            throw SqlitePluginException(column, "Column name 'table_name' forbidden")
          }

          typeSpec.addField(FieldSpec.builder(String::class.java, column.constantName)
              .addModifiers(PUBLIC, STATIC, FINAL)
              .initializer("\$S", column.name)
              .build())

          val methodSpec = MethodSpec.methodBuilder(column.methodName)
              .returns(column.javaType)
              .addModifiers(PUBLIC, ABSTRACT)
          if (column.isNullable) {
            methodSpec.addAnnotation(NULLABLE)
          }
          typeSpec.addMethod(methodSpec.build())
        }

        typeSpec.addField(FieldSpec.builder(String::class.java, CREATE_TABLE)
            .addModifiers(PUBLIC, STATIC, FINAL)
            .initializer("\"\"\n    + \$S", table.sqliteText()) // Start SQL on wrapped line.
            .build())

        val interfaceClassName = ClassName.get(packageName, className)
        typeSpec.addType(MapperSpec.builder(table, interfaceClassName).build())
            .addType(MarshalSpec.builder(table, interfaceClassName, fileName).build())
      }

      parseContext.sql_stmt_list().sql_stmt().forEach {
        if (it.identifier == CREATE_TABLE) {
          throw SqlitePluginException(it.sql_stmt_name(), "'CREATE_TABLE' identifier is reserved")
        }
        typeSpec.addField(FieldSpec.builder(String::class.java, it.identifier)
            .addModifiers(PUBLIC, STATIC, FINAL)
            .initializer("\"\"\n    + \$S", it.body().sqliteText()) // Start SQL on wrapped line.
            .build())
      }

      val javaFile = JavaFile.builder(packageName, typeSpec.build()).build()

      val buildDirectory = File(File(projectPath, "build"), SqliteCompiler.OUTPUT_DIRECTORY)
      val packageDirectory = File(buildDirectory, packageName.replace('.', File.separatorChar))
      packageDirectory.mkdirs()
      val outputFile = File(packageDirectory, className + ".java")
      outputFile.createNewFile()
      javaFile.writeTo(PrintStream(FileOutputStream(outputFile)))

      return Status.Success(parseContext, outputFile)
    } catch (e: SqlitePluginException) {
      return Status.Failure(e.originatingElement, e.message)
    } catch (e: IOException) {
      return Status.Failure(parseContext, e.message ?: "IOException occurred")
    }
  }

  companion object {
    const val TABLE_NAME = "TABLE_NAME"
    const val CREATE_TABLE = "CREATE_TABLE"
    const val FILE_EXTENSION = "sq"
    val OUTPUT_DIRECTORY = "generated${File.separatorChar}source${File.separatorChar}sqldelight"
    val NULLABLE = ClassName.get("android.support.annotation", "Nullable")
    val COLUMN_ADAPTER_TYPE = ClassName.get("com.squareup.sqldelight", "ColumnAdapter")

    fun interfaceName(sqliteFileName: String) = sqliteFileName + "Model"
    fun constantName(name: String) = name.toUpperCase(US)
  }
}
