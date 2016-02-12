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
import com.squareup.sqldelight.SqliteCompiler.Status.Result.FAILURE
import com.squareup.sqldelight.SqliteCompiler.Status.Result.SUCCESS
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

class SqliteCompiler<T> {
  fun write(tableGenerator: TableGenerator<T, *, *, *, *>): Status<T> {
    try {
      val columnFieldNames = linkedSetOf<String>()
      val typeSpec = TypeSpec.interfaceBuilder(tableGenerator.generatedFileName)
          .addModifiers(PUBLIC)
      if (tableGenerator.table != null) {
        typeSpec.addField(FieldSpec.builder(String::class.java, TABLE_NAME)
            .addModifiers(PUBLIC, STATIC, FINAL)
            .initializer("\$S", tableGenerator.table.sqlTableName)
            .build())

        for (column in tableGenerator.table.columns) {
          if (columnFieldNames.contains(column.fieldName)) {
            return Status(column.originatingElement, "Duplicate column name", FAILURE)
          }
          columnFieldNames.add(column.fieldName);

          typeSpec.addField(FieldSpec.builder(String::class.java, column.fieldName)
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

        typeSpec.addType(MapperSpec.builder(tableGenerator.table).build())
            .addType(MarshalSpec.builder(tableGenerator.table).build())
      }

      val sqlFieldNames = linkedSetOf<String>()
      for (sqlStmt in tableGenerator.sqliteStatements) {
        if (columnFieldNames.contains(sqlStmt.identifier)) {
          return Status(sqlStmt.originatingElement, "SQL identifier collides with column name",
              FAILURE)
        }
        if (sqlFieldNames.contains(sqlStmt.identifier)) {
          return Status(sqlStmt.originatingElement, "Duplicate SQL identifier", FAILURE)
        }
        sqlFieldNames.add(sqlStmt.identifier)

        typeSpec.addField(FieldSpec.builder(String::class.java, sqlStmt.identifier)
            .addModifiers(PUBLIC, STATIC, FINAL)
            .initializer("\"\"\n    + \$S", sqlStmt.stmt) // Start SQL on wrapped line.
            .build())
      }

      val javaFile = JavaFile.builder(tableGenerator.packageName, typeSpec.build()).build()
      val outputDirectory = tableGenerator.fileDirectory
      outputDirectory.mkdirs()
      val outputFile = File(outputDirectory, tableGenerator.generatedFileName + ".java")
      outputFile.createNewFile()
      javaFile.writeTo(PrintStream(FileOutputStream(outputFile)))

      return Status(tableGenerator.originatingElement, "", SUCCESS)
    } catch (e: SqlitePluginException) {
      return Status(e.originatingElement as T, e.message, FAILURE)
    } catch (e: IOException) {
      return Status(tableGenerator.originatingElement, e.message, FAILURE)
    }
  }

  class Status<R>(val originatingElement: R, val errorMessage: String?, val result: Result) {
    enum class Result {
      SUCCESS, FAILURE
    }
  }

  companion object {
    const val TABLE_NAME = "TABLE_NAME"
    const val OUTPUT_DIRECTORY = "generated/source/sqldelight"
    const val FILE_EXTENSION = "sq"
    val NULLABLE = ClassName.get("android.support.annotation", "Nullable")
    val COLUMN_ADAPTER_TYPE = ClassName.get("com.squareup.sqldelight", "ColumnAdapter")

    fun interfaceName(sqliteFileName: String) = sqliteFileName + "Model"
  }
}
