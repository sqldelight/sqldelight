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

import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.openapi.module.Module
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier.OPERATOR
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.sqldelight.core.SqlDelightException
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.compiler.integration.adapterProperty
import com.squareup.sqldelight.core.compiler.integration.needsAdapters
import com.squareup.sqldelight.core.lang.DATABASE_SCHEMA_TYPE
import com.squareup.sqldelight.core.lang.DRIVER_NAME
import com.squareup.sqldelight.core.lang.DRIVER_TYPE
import com.squareup.sqldelight.core.lang.MigrationFile
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.SqlDelightQueriesFile
import com.squareup.sqldelight.core.lang.TRANSACTER_IMPL_TYPE
import com.squareup.sqldelight.core.lang.TRANSACTER_TYPE
import com.squareup.sqldelight.core.lang.queriesImplType
import com.squareup.sqldelight.core.lang.queriesName
import com.squareup.sqldelight.core.lang.queriesType
import com.squareup.sqldelight.core.lang.util.allowsReferenceCycles
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.forInitializationStatements
import com.squareup.sqldelight.core.lang.util.rawSqlText

internal class DatabaseGenerator(
  module: Module,
  sourceFile: SqlDelightFile
) {
  private val sourceFolders = SqlDelightFileIndex.getInstance(module).sourceFolders(sourceFile)
  private val moduleFolders = SqlDelightFileIndex.getInstance(module)
    .sourceFolders(sourceFile, includeDependencies = false)
  private val fileIndex = SqlDelightFileIndex.getInstance(module)
  private val type = ClassName(fileIndex.packageName, fileIndex.className)
  private val dialect = sourceFile.dialect

  fun interfaceType(): TypeSpec {
    val typeSpec = TypeSpec.interfaceBuilder(fileIndex.className)
      .addSuperinterface(TRANSACTER_TYPE)

    fileIndex.dependencies.forEach {
      typeSpec.addSuperinterface(ClassName(it.packageName, it.className))
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

    moduleFolders.flatMap { it.findChildrenOfType<SqlDelightQueriesFile>() }
      .sortedBy { it.name }
      .forEach { file ->
        // queries property added to QueryWrapper type:
        // val dataQueries = DataQueries(this, driver, transactions)
        typeSpec.addProperty(file.queriesName, file.queriesType)
      }

    forAdapters {
      invoke.addParameter(it.name, it.type)
      invokeReturn.add(", %L", it.name)
    }

    return typeSpec
      .addType(
        TypeSpec.companionObjectBuilder()
          .addProperty(
            PropertySpec.builder("Schema", DATABASE_SCHEMA_TYPE)
              .getter(
                FunSpec.getterBuilder()
                  .addStatement("return %T::class.schema", type)
                  .build()
              )
              .build()
          )
          .addFunction(
            invoke
              .addCode(
                invokeReturn
                  .add(")")
                  .build()
              )
              .build()
          )
          .build()
      )
      .build()
  }

  private fun forAdapters(
    block: (PropertySpec) -> Unit
  ) {
    val queriesFile = sourceFolders.flatMap { it.findChildrenOfType<SqlDelightQueriesFile>() }
      .firstOrNull() ?: return
    queriesFile.tables(true)
      .toSet()
      .mapNotNull { if (it.needsAdapters()) it.adapterProperty() else null }
      .sortedBy { it.name }
      .forEach(block)
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

    forAdapters {
      typeSpec.addProperty(it)
      constructor.addParameter(it.name, it.type)
    }

    sourceFolders.flatMap { it.findChildrenOfType<SqlDelightQueriesFile>() }
      .sortedBy { it.name }
      .forEach { file ->
        // queries property added to QueryWrapper type:
        // val dataQueries = DataQueries(this, driver, transactions)
        typeSpec.addProperty(
          PropertySpec.builder(file.queriesName, file.queriesImplType(implementationPackage))
            .addModifiers(OVERRIDE)
            .initializer("%T(this, $DRIVER_NAME)", file.queriesImplType(implementationPackage))
            .build()
        )
      }

    if (!fileIndex.deriveSchemaFromMigrations) {
      // Derive the schema from queries files.
      sourceFolders.flatMap { it.findChildrenOfType<SqlDelightQueriesFile>() }
        .forInitializationStatements(dialect.allowsReferenceCycles) { sqlText ->
          createFunction.addStatement("$DRIVER_NAME.execute(null, %L, 0)", sqlText.toCodeLiteral())
        }
    } else {
      val orderedMigrations = sourceFolders.flatMap { it.findChildrenOfType<MigrationFile>() }
        .sortedBy { it.order }

      // Derive the schema from migration files.
      orderedMigrations.flatMap { it.sqliteStatements() }
        .filter { it.isSchema() }
        .forEach {
          createFunction.addStatement("$DRIVER_NAME.execute(null, %L, 0)", it.rawSqlText().toCodeLiteral())
        }
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
      .addType(
        TypeSpec.objectBuilder(DATABASE_SCHEMA_TYPE.simpleName)
          .addSuperinterface(DATABASE_SCHEMA_TYPE)
          .addFunction(createFunction.build())
          .addFunction(migrateFunction.build())
          .addProperty(
            PropertySpec.builder("version", INT, OVERRIDE)
              .getter(FunSpec.getterBuilder().addStatement("return $maxVersion").build())
              .build()
          )
          .build()
      )
      .addSuperinterface(ClassName(fileIndex.packageName, fileIndex.className))
      .primaryConstructor(constructor.build())
      .build()
  }

  private fun SqlStmt.isSchema() = when {
    createIndexStmt != null -> true
    createTableStmt != null -> true
    createTriggerStmt != null -> true
    createViewStmt != null -> true
    createVirtualTableStmt != null -> true
    alterTableStmt != null -> true
    dropIndexStmt != null -> true
    dropTableStmt != null -> true
    dropTriggerStmt != null -> true
    dropViewStmt != null -> true
    else -> false
  }
}
