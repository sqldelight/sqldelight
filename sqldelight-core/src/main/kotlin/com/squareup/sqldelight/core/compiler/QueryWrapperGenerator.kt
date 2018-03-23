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
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import com.squareup.sqldelight.core.lang.ADAPTER_NAME
import com.squareup.sqldelight.core.lang.CONNECTION_NAME
import com.squareup.sqldelight.core.lang.CONNECTION_TYPE
import com.squareup.sqldelight.core.lang.DATABASE_NAME
import com.squareup.sqldelight.core.lang.DATABASE_TYPE
import com.squareup.sqldelight.core.lang.QUERY_WRAPPER_NAME
import com.squareup.sqldelight.core.lang.STATEMENT_TYPE_ENUM
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.THREADLOCAL_TYPE
import com.squareup.sqldelight.core.lang.TRANSACTIONS_NAME
import com.squareup.sqldelight.core.lang.TRANSACTION_TYPE
import com.squareup.sqldelight.core.lang.adapterName
import com.squareup.sqldelight.core.lang.queriesName
import com.squareup.sqldelight.core.lang.queriesType
import com.squareup.sqldelight.core.lang.util.columns
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.rawSqlText

internal class QueryWrapperGenerator(module: Module) {
  val sourceFolders = SqlDelightFileIndex.getInstance(module).sourceFolders()

  fun type(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder(QUERY_WRAPPER_NAME.capitalize())

    val constructor = FunSpec.constructorBuilder()

    // Database constructor parameter:
    // database: SqlDatabase
    val dbParameter = ParameterSpec.builder(DATABASE_NAME, DATABASE_TYPE).build()
    constructor.addParameter(dbParameter)

    // transactions property:
    // private val transactions = ThreadLocal<Transacter.Transaction>()
    val transactionsType = ParameterizedTypeName.get(
        rawType = THREADLOCAL_TYPE,
        typeArguments = TRANSACTION_TYPE
    )
    typeSpec.addProperty(PropertySpec.builder(TRANSACTIONS_NAME, transactionsType, PRIVATE)
        .initializer("%T()", transactionsType)
        .build())

    // Static on create function:
    // fun onCreate(db: SqlDatabaseConnection)
    val onCreateFunction = FunSpec.builder("onCreate")
        .addParameter(CONNECTION_NAME, CONNECTION_TYPE)

    sourceFolders.flatMap { it.findChildrenOfType<SqlDelightFile>() }
        .forEach { file ->
          // queries property added to QueryWrapper type:
          // val dataQueries = DataQueries(this, database, transactions)
          typeSpec.addProperty(PropertySpec.builder(file.queriesName, file.queriesType)
              .initializer("%T(this, $DATABASE_NAME, $TRANSACTIONS_NAME)", file.queriesType)
              .build())

          file.sqliteStatements().forEach statements@{ (label, sqliteStatement) ->
            if (label.name != null) return@statements

            // Unlabeled statements are run during onCreate callback:
            // db.prepareStatement("CREATE TABLE ... ", Type.EXEC).execute()
            onCreateFunction.addStatement(
                "$CONNECTION_NAME.prepareStatement(%S, %T.EXEC).execute()",
                sqliteStatement.rawSqlText(), STATEMENT_TYPE_ENUM
            )

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

    return typeSpec
        .primaryConstructor(constructor.build())
        .companionObject(TypeSpec.companionObjectBuilder()
            .addFunction(onCreateFunction.build())
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
