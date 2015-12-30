package com.alecstrong.sqlite.android

import com.alecstrong.sqlite.android.model.Table
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

class SqliteCompiler<T> {
  fun write(tableGenerator: TableGenerator<T, *, *, *, *>): Status<T> {
    if (tableGenerator.packageName == null) {
      return Status(tableGenerator.originatingElement,
          "Expected but did not find package statement", Status.Result.FAILURE)
    }
    try {
      val typeSpec = TypeSpec.interfaceBuilder(tableGenerator.generatedFileName)
          .addModifiers(PUBLIC)
      if (tableGenerator.table != null) {
        typeSpec.addField(FieldSpec.builder(ClassName.get(String::class.java), TABLE_NAME)
            .addModifiers(PUBLIC, STATIC, FINAL)
            .initializer("${'$'}S", tableGenerator.table.sqlTableName)
            .build())

        for (column in tableGenerator.table.columns) {
          typeSpec.addField(FieldSpec.builder(ClassName.get(String::class.java), column.fieldName)
              .addModifiers(PUBLIC, STATIC, FINAL)
              .initializer("${'$'}S", column.name)
              .build())

          val methodSpec = MethodSpec.methodBuilder(column.methodName)
              .returns(column.javaType)
              .addModifiers(PUBLIC, Modifier.ABSTRACT)
          if (column.isNullable) {
            methodSpec.addAnnotation(ClassName.get("android.support.annotation", "Nullable"))
          }
          typeSpec.addMethod(methodSpec.build())
        }

        if (tableGenerator.table.isKeyValue) {
          typeSpec.addField(keyValueQuery(tableGenerator.table))
        }

        typeSpec.addType(MapperSpec.builder(tableGenerator.table).build())
            .addType(MarshalSpec.builder(tableGenerator.table).build())
      }

      for (sqlStmt in tableGenerator.sqliteStatements) {
        typeSpec.addField(FieldSpec.builder(ClassName.get(String::class.java), sqlStmt.identifier)
            .addModifiers(PUBLIC, STATIC, FINAL)
            .initializer("\"\"\n    + ${'$'}S", sqlStmt.stmt) // Start SQL on wrapped line.
            .build())
      }

      val javaFile = JavaFile.builder(tableGenerator.packageName, typeSpec.build()).build()
      val outputDirectory = tableGenerator.fileDirectory
      outputDirectory.mkdirs()
      val outputFile = File(outputDirectory, tableGenerator.generatedFileName + ".java")
      outputFile.createNewFile()
      javaFile.writeTo(PrintStream(FileOutputStream(outputFile)))

      return Status(tableGenerator.originatingElement, "", Status.Result.SUCCESS)
    } catch (e: SqlitePluginException) {
      return Status(e.originatingElement as T, e.message, Status.Result.FAILURE)
    } catch (e: IOException) {
      return Status(tableGenerator.originatingElement, e.message, Status.Result.FAILURE)
    }
  }

  internal fun keyValueQuery(table: Table<T>) =
      FieldSpec.builder(ClassName.get(String::class.java), "QUERY", PUBLIC, STATIC, FINAL)
          .initializer("\"\"\n    + ${'$'}S", "" +
              "SELECT *\n" +
              "  FROM ${table.sqlTableName}\n" +
              " WHERE key IN (${table.columns.map({ "'${it.name}'" }).joinToString()})")
          .build()

  class Status<R>(val originatingElement: R, val errorMessage: String?, val result: Status.Result) {
    enum class Result {
      SUCCESS, FAILURE
    }
  }

  companion object {
    const val TABLE_NAME = "TABLE_NAME"
    const val KEY_VALUE_KEY_COLUMN = "key"
    const val KEY_VALUE_VALUE_COLUMN = "value"
    const val OUTPUT_DIRECTORY: String = "generated/source/sqlite"
    const val FILE_EXTENSION: String = "sq"

    fun interfaceName(sqliteFileName: String) = sqliteFileName + "Model"
  }
}
