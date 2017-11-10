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
import com.intellij.openapi.project.Project
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.util.columns
import com.squareup.sqldelight.core.lang.util.findChildrenOfType

internal class DatabaseGenerator(project: Project) {
  val sourceFolders = SqlDelightFileIndex.getInstance(project).sourceFolders()

  fun databaseSpec(): TypeSpec {
    val typeSpec = TypeSpec.classBuilder("Database")
        .superclass(SQLDELIGHT_DATABASE_TYPE)
    val openHelper = ParameterSpec.builder("openHelper", OPEN_HELPER_TYPE).build()
    val constructor = FunSpec.constructorBuilder()
        .addParameter(openHelper)
    val dbParameter = ParameterSpec.builder("db", DATABASE_TYPE).build()
    val onCreateCode = CodeBlock.builder()

    sourceFolders.flatMap { it.findChildrenOfType<SqlDelightFile>() }
        .forEach { sqlDelightFile ->
          sqlDelightFile.sqliteStatements().forEach statements@{ (label, sqliteStatement) ->
            if (label.name != null) return@statements

            // Unlabeled statements are run during onCreate callback.
            onCreateCode.addStatement("%N.execSql(%S)", dbParameter, sqliteStatement.text)

            sqliteStatement.createTableStmt?.let {
              if (it.columns.any { it.adapter() != null }) {
                // Database object needs an adapter reference for this table type.
                val property = adapterProperty(sqlDelightFile.packageName, it)
                typeSpec.addProperty(property)
                constructor.addParameter(property.name, property.type)
              }
            }
          }
        }

    val versionParam = ParameterSpec.builder("version", INT).build()

    val callback = TypeSpec.anonymousClassBuilder("%N", versionParam)
        .superclass(CALLBACK_TYPE)
        .addFunction(FunSpec.builder("onCreate")
            .addParameter(dbParameter)
            .addModifiers(OVERRIDE)
            .addCode(onCreateCode.build())
            .build())
        .build()

    return typeSpec.addSuperclassConstructorParameter("%N", openHelper)
        .primaryConstructor(constructor.build())
        .companionObject(TypeSpec.companionObjectBuilder()
            .addFunction(FunSpec.builder("callback")
                .returns(CALLBACK_TYPE)
                .addParameter(versionParam)
                .addStatement("return %L", callback)
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
        createTable.tableName.name.capitalize(),
        TableInterfaceGenerator.ADAPTER_NAME
    )
    val adapterName = "${createTable.tableName.name}${TableInterfaceGenerator.ADAPTER_NAME}"
    return PropertySpec.builder(adapterName, adapterType, INTERNAL)
        .initializer(adapterName)
        .build()
  }

  companion object {
    private val OPEN_HELPER_TYPE = ClassName("android.arch.persistence.db", "SupportSQLiteOpenHelper")
    private val CALLBACK_TYPE = ClassName("android.arch.persistence.db", "SupportSQLiteOpenHelper", "Callback")
    private val DATABASE_TYPE = ClassName("android.arch.persistence.db", "SupportSQLiteDatabase")
    private val SQLDELIGHT_DATABASE_TYPE = ClassName("com.squareup.sqldelight", "SqlDelightDatabase")
  }
}
