/*
 * Copyright (C) 2017 Square, Inc.
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
package com.squareup.sqldelight.core.compiler

import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.intellij.openapi.module.Module
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.sqldelight.core.SqlDelightException
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import com.squareup.sqldelight.core.lang.ADAPTER_NAME
import com.squareup.sqldelight.core.lang.CONNECTION_NAME
import com.squareup.sqldelight.core.lang.CONNECTION_TYPE
import com.squareup.sqldelight.core.lang.DATABASE_SCHEMA_TYPE
import com.squareup.sqldelight.core.lang.DATABASE_NAME
import com.squareup.sqldelight.core.lang.DATABASE_TYPE
import com.squareup.sqldelight.core.lang.MigrationFile
import com.squareup.sqldelight.core.lang.QUERY_WRAPPER_NAME
import com.squareup.sqldelight.core.lang.STATEMENT_TYPE_ENUM
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.adapterName
import com.squareup.sqldelight.core.lang.queriesName
import com.squareup.sqldelight.core.lang.queriesType
import com.squareup.sqldelight.core.lang.util.columns
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.forInitializationStatements
import com.squareup.sqldelight.core.lang.util.rawSqlText

internal class QueryWrapperGenerator(module: Module, sourceFile: SqlDelightFile) {
  val sourceFolders = SqlDelightFileIndex.getInstance(module).sourceFolders(sourceFile)

  fun type(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder(QUERY_WRAPPER_NAME.capitalize())

    val constructor = FunSpec.constructorBuilder()

    // Database constructor parameter:
    // database: SqlDatabase
    val dbParameter = ParameterSpec.builder(DATABASE_NAME, DATABASE_TYPE).build()
    constructor.addParameter(dbParameter)

    // Static on create function:
    // fun create(db: SqlDatabaseConnection)
    val createFunction = FunSpec.builder("create")
        .addModifiers(OVERRIDE)
        .addParameter(CONNECTION_NAME, CONNECTION_TYPE)

    val oldVersion = ParameterSpec.builder("oldVersion", INT).build()
    val newVersion = ParameterSpec.builder("newVersion", INT).build()

    val migrateFunction = FunSpec.builder("migrate")
        .addModifiers(OVERRIDE)
        .addParameter(CONNECTION_NAME, CONNECTION_TYPE)
        .addParameter(oldVersion)
        .addParameter(newVersion)

    sourceFolders.flatMap { it.findChildrenOfType<SqlDelightFile>() }
        .forEach { file ->
          // queries property added to QueryWrapper type:
          // val dataQueries = DataQueries(this, database, transactions)
          typeSpec.addProperty(PropertySpec.builder(file.queriesName, file.queriesType)
              .initializer("%T(this, $DATABASE_NAME)", file.queriesType)
              .build())

          file.sqliteStatements().forEach statements@{ (label, sqliteStatement) ->
            if (label.name != null) return@statements

            sqliteStatement.createTableStmt?.let {
              if (it.columns.any { it.adapter() != null }) {
                // Database object needs an adapter reference for this table type.
                val property = adapterProperty(file.packageName, it)
                typeSpec.addProperty(property)
                constructor.addParameter(property.name, property.type)
              }
            }
          }
        }

    sourceFolders.flatMap { it.findChildrenOfType<SqlDelightFile>() }
        .forInitializationStatements { sqlText ->
          createFunction.addStatement(
              "$CONNECTION_NAME.prepareStatement(%S, %T.EXECUTE, 0).execute()",
              sqlText, STATEMENT_TYPE_ENUM
          )
        }

    var maxVersion = 1

    sourceFolders.flatMap { it.findChildrenOfType<MigrationFile>() }
        .sortedBy { it.version }
        .forEach { migrationFile ->
          try {
            maxVersion = maxOf(maxVersion, migrationFile.version + 1)
          } catch (e: Throwable) {
            throw SqlDelightException("Migration files can only have versioned names (1.sqm, 2.sqm, etc)")
          }
          migrateFunction.beginControlFlow(
              "if (%N <= ${migrationFile.version} && %N > ${migrationFile.version})",
              oldVersion, newVersion
          )
          migrationFile.sqliteStatements().forEach {
            migrateFunction.addStatement(
                "$CONNECTION_NAME.prepareStatement(%S, %T.EXECUTE, 0).execute()",
                it.rawSqlText(), STATEMENT_TYPE_ENUM
            )
          }
          migrateFunction.endControlFlow()
        }

    return typeSpec
        .primaryConstructor(constructor.build())
        .addType(TypeSpec.objectBuilder(DATABASE_SCHEMA_TYPE.simpleName)
            .addSuperinterface(DATABASE_SCHEMA_TYPE)
            .addFunction(createFunction.build())
            .addFunction(migrateFunction.build())
            .addProperty(PropertySpec.builder("version", INT, OVERRIDE)
                .getter(FunSpec.getterBuilder().addStatement("return $maxVersion").build())
                .build())
            .build())
        .build()
  }

  private fun adapterProperty(
      packageName: String,
      createTable: SqliteCreateTableStmt
  ): PropertySpec {
    val adapterType = ClassName(
        packageName,
        allocateName(createTable.tableName).capitalize(),
        ADAPTER_NAME
    )
    return PropertySpec.builder(createTable.adapterName, adapterType, INTERNAL)
        .initializer(createTable.adapterName)
        .build()
  }
}
