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

import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.openapi.module.Module
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.OPERATOR
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.sqldelight.core.SqlDelightException
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler.allocateName
import com.squareup.sqldelight.core.lang.ADAPTER_NAME
import com.squareup.sqldelight.core.lang.DATABASE_SCHEMA_TYPE
import com.squareup.sqldelight.core.lang.DRIVER_NAME
import com.squareup.sqldelight.core.lang.DRIVER_TYPE
import com.squareup.sqldelight.core.lang.MigrationFile
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.TRANSACTER_IMPL_TYPE
import com.squareup.sqldelight.core.lang.TRANSACTER_TYPE
import com.squareup.sqldelight.core.lang.adapterName
import com.squareup.sqldelight.core.lang.queriesImplType
import com.squareup.sqldelight.core.lang.queriesName
import com.squareup.sqldelight.core.lang.queriesType
import com.squareup.sqldelight.core.lang.util.columns
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.forInitializationStatements
import com.squareup.sqldelight.core.lang.util.rawSqlText

internal class QueryWrapperGenerator(
  module: Module,
  sourceFile: SqlDelightFile
) {
  private val sourceFolders = SqlDelightFileIndex.getInstance(module).sourceFolders(sourceFile)
  private val moduleFolders = SqlDelightFileIndex.getInstance(module)
      .sourceFolders(sourceFile, includeDependencies = false)
  private val fileIndex = SqlDelightFileIndex.getInstance(module)
  private val type = ClassName(fileIndex.packageName, fileIndex.className)

  fun interfaceType(): TypeSpec {
    val typeSpec = TypeSpec.interfaceBuilder(fileIndex.className)
        .addSuperinterface(TRANSACTER_TYPE)

    fileIndex.dependencies.forEach { (packageName, className) ->
      typeSpec.addSuperinterface(ClassName(packageName, className))
    }

    val invoke = FunSpec.builder("invoke")
        .returns(ClassName(fileIndex.packageName, fileIndex.className))
        .addModifiers(OPERATOR)

    val invokeReturn = CodeBlock.builder()
        .add("return %T::class.newInstance(", type)

    // Database constructor parameter:
    // driver: SqlDriver
    val dbParameter = ParameterSpec.builder(DRIVER_NAME, DRIVER_TYPE).build()
    invoke.addParameter(dbParameter)
    invokeReturn.add("%N", dbParameter)

    moduleFolders.flatMap { it.findChildrenOfType<SqlDelightFile>() }
        .sortedBy { it.name }
        .forEach { file ->
          // queries property added to QueryWrapper type:
          // val dataQueries = DataQueries(this, driver, transactions)
          typeSpec.addProperty(file.queriesName, file.queriesType)
        }

    sourceFolders.flatMap { it.findChildrenOfType<SqlDelightFile>() }
        .sortedBy { it.name }
        .forEach { file ->
          file.sqliteStatements().forEach statements@{ (label, sqliteStatement) ->
            if (label.name != null) return@statements

            sqliteStatement.createTableStmt?.let {
              if (it.columns.any { it.adapter() != null }) {
                // Database object needs an adapter reference for this table type.
                val property = adapterProperty(file.packageName, it)
                invoke.addParameter(property.name, property.type)
                invokeReturn.add(", %L", property.name)
              }
            }
          }
        }

    return typeSpec
        .addType(TypeSpec.companionObjectBuilder()
            .addProperty(PropertySpec.builder("Schema", DATABASE_SCHEMA_TYPE)
                .getter(FunSpec.getterBuilder()
                    .addStatement("return %T::class.schema", type)
                    .build())
                .build())
            .addFunction(invoke
                .addCode(invokeReturn
                    .add(")")
                    .build())
                .build())
            .build())
        .build()
  }

  fun type(implementationPackage: String): TypeSpec {
    val typeSpec = TypeSpec.classBuilder("${fileIndex.className}Impl")
        .superclass(TRANSACTER_IMPL_TYPE)
        .addModifiers(PRIVATE)
        .addSuperclassConstructorParameter(DRIVER_NAME)

    val constructor = FunSpec.constructorBuilder()

    // Database constructor parameter:
    // driver: SqlDriver
    val dbParameter = ParameterSpec.builder(DRIVER_NAME, DRIVER_TYPE).build()
    constructor.addParameter(dbParameter)

    // Static on create function:
    // fun create(driver: SqlDriver)
    val createFunction = FunSpec.builder("create")
        .addModifiers(OVERRIDE)
        .addParameter(DRIVER_NAME, DRIVER_TYPE)

    val oldVersion = ParameterSpec.builder("oldVersion", INT).build()
    val newVersion = ParameterSpec.builder("newVersion", INT).build()

    val migrateFunction = FunSpec.builder("migrate")
        .addModifiers(OVERRIDE)
        .addParameter(DRIVER_NAME, DRIVER_TYPE)
        .addParameter(oldVersion)
        .addParameter(newVersion)

    sourceFolders.flatMap { it.findChildrenOfType<SqlDelightFile>() }
        .sortedBy { it.name }
        .forEach { file ->
          // queries property added to QueryWrapper type:
          // val dataQueries = DataQueries(this, driver, transactions)
          typeSpec.addProperty(PropertySpec.builder(file.queriesName, file.queriesImplType(implementationPackage))
              .addModifiers(OVERRIDE)
              .initializer("%T(this, $DRIVER_NAME)", file.queriesImplType(implementationPackage))
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
          createFunction.addStatement("$DRIVER_NAME.execute(null, %L, 0)", sqlText.toCodeLiteral())
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
            migrateFunction.addStatement("$DRIVER_NAME.execute(null, %S, 0)", it.rawSqlText())
          }
          migrateFunction.endControlFlow()
        }

    return typeSpec
        .addType(TypeSpec.objectBuilder(DATABASE_SCHEMA_TYPE.simpleName)
            .addSuperinterface(DATABASE_SCHEMA_TYPE)
            .addFunction(createFunction.build())
            .addFunction(migrateFunction.build())
            .addProperty(PropertySpec.builder("version", INT, OVERRIDE)
                .getter(FunSpec.getterBuilder().addStatement("return $maxVersion").build())
                .build())
            .build())
        .addSuperinterface(ClassName(fileIndex.packageName, fileIndex.className))
        .primaryConstructor(constructor.build())
        .build()
  }

  private fun adapterProperty(
      packageName: String,
      createTable: SqlCreateTableStmt
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
