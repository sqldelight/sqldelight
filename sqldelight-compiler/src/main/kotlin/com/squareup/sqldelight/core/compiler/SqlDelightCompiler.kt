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
package com.squareup.sqldelight.core.compiler

import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.SqlCreateViewStmt
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.compiler.model.NamedQuery
import com.squareup.sqldelight.core.lang.MigrationFile
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.SqlDelightQueriesFile
import com.squareup.sqldelight.core.lang.queriesName
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.sqFile
import java.io.Closeable

private typealias FileAppender = (fileName: String) -> Appendable

object SqlDelightCompiler {
  fun writeInterfaces(
    module: Module,
    file: SqlDelightQueriesFile,
    implementationFolder: String,
    output: FileAppender
  ) {
    writeTableInterfaces(module, file, implementationFolder, output)
    writeViewInterfaces(module, file, implementationFolder, output)
    writeQueryInterfaces(module, file, implementationFolder, output)
    writeQueriesInterface(module, file, implementationFolder, output)
  }

  fun writeInterfaces(
    module: Module,
    file: MigrationFile,
    implementationFolder: String,
    output: FileAppender,
    includeAll: Boolean = false
  ) {
    writeTableInterfaces(module, file, implementationFolder, output, includeAll)
    writeViewInterfaces(module, file, implementationFolder, output, includeAll)
  }

  fun writeDatabaseInterface(
    module: Module,
    file: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender
  ) {
    writeQueryWrapperInterface(module, file, implementationFolder, output)
  }

  fun writeImplementations(
    module: Module,
    sourceFile: SqlDelightQueriesFile,
    implementationFolder: String,
    output: FileAppender
  ) {
    val fileIndex = SqlDelightFileIndex.getInstance(module)
    val packageName = "${fileIndex.packageName}.$implementationFolder"
    val outputDirectory = "${fileIndex.outputDirectory}/${packageName.replace(".", "/")}"
    val databaseImplementationType = DatabaseGenerator(module, sourceFile).type(packageName)
    val exposer = DatabaseExposerGenerator(databaseImplementationType, fileIndex)

    FileSpec.builder(packageName, databaseImplementationType.name!!)
        .addProperty(exposer.exposedSchema())
        .addFunction(exposer.exposedConstructor())
        .addType(databaseImplementationType)
        .apply {
          fileIndex.sourceFolders(sourceFile, includeDependencies = true)
              .flatMap { it.findChildrenOfType<SqlDelightQueriesFile>() }
              .forEach { file ->
                val queriesGenerator = QueriesTypeGenerator(module, file)
                addType(queriesGenerator.generateType(packageName))
              }
        }
        .build()
        .writeToAndClose(output("$outputDirectory/${databaseImplementationType.name}.kt"))
  }

  private fun writeQueryWrapperInterface(
    module: Module,
    sourceFile: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender
  ) {
    val fileIndex = SqlDelightFileIndex.getInstance(module)
    val packageName = fileIndex.packageName
    val outputDirectory = "${fileIndex.outputDirectory}/${packageName.replace(".", "/")}"
    val queryWrapperType = DatabaseGenerator(module, sourceFile).interfaceType()
    FileSpec.builder(packageName, queryWrapperType.name!!)
        // TODO: Remove these when kotlinpoet supports top level types.
        .addImport("$packageName.$implementationFolder", "newInstance", "schema")
        .apply {
          var index = 0
          fileIndex.dependencies.forEach {
            addAliasedImport(ClassName(it.packageName, it.className), "${it.className}${index++}")
          }
        }
        .addType(queryWrapperType)
        .build()
        .writeToAndClose(output("$outputDirectory/${queryWrapperType.name}.kt"))
  }

  internal fun writeTableInterfaces(
    module: Module,
    file: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender,
    includeAll: Boolean = false
  ) {
    val packageName = file.packageName ?: return
    file.tables(includeAll).forEach { query ->
      val statement = query.tableName.parent
      if (statement is SqlCreateViewStmt) return@forEach

      FileSpec.builder(packageName, allocateName(query.tableName))
          .apply {
            tryWithElement(statement) {
              val generator = TableInterfaceGenerator(query)
              addType(generator.kotlinImplementationSpec())
            }
          }
          .build()
          .writeToAndClose(output("${statement.sqFile().generatedDir}/${allocateName(query.tableName).capitalize()}.kt"))
    }
  }

  internal fun writeViewInterfaces(
    module: Module,
    file: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender,
    includeAll: Boolean = false
  ) {
    file.views(includeAll)
        .filter { it.compoundSelectStmt != null }
        .map { NamedQuery(allocateName(it.viewName), it.compoundSelectStmt!!, it.viewName) }
        .writeQueryInterfaces(file, output)
  }

  internal fun writeQueryInterfaces(
    module: Module,
    file: SqlDelightQueriesFile,
    implementationFolder: String,
    output: FileAppender
  ) {
    file.namedQueries.writeQueryInterfaces(file, output)
  }

  internal fun writeQueriesInterface(
    module: Module,
    file: SqlDelightQueriesFile,
    implementationFolder: String,
    output: FileAppender
  ) {
    val packageName = file.packageName ?: return
    val queriesType = QueriesTypeGenerator(module, file).interfaceType()
    FileSpec.builder(packageName, file.queriesName.capitalize())
        .addType(queriesType)
        .build()
        .writeToAndClose(output("${file.generatedDir}/${queriesType.name}.kt"))
  }

  internal fun allocateName(namedElement: NamedElement): String {
    return NameAllocator().newName(namedElement.normalizedName)
  }

  private fun List<NamedQuery>.writeQueryInterfaces(file: SqlDelightFile, output: FileAppender) {
    val packageName = file.packageName ?: return
    return filter { tryWithElement(it.select) { it.needsInterface() } }
        .forEach { namedQuery ->
          FileSpec.builder(packageName, namedQuery.name)
              .apply {
                tryWithElement(namedQuery.select) {
                  val generator = QueryInterfaceGenerator(namedQuery)
                  addType(generator.kotlinImplementationSpec())
                }
              }
              .build()
              .writeToAndClose(output("${file.generatedDir}/${namedQuery.name.capitalize()}.kt"))
        }
  }

  private val NamedElement.normalizedName: String
    get() {
      val f = name[0]
      val l = name[name.lastIndex]
      return if ((f in "\"'`" && f == l) || (f == '[' && l == ']')) {
        name.substring(1, name.length - 1)
      } else {
        name
      }
    }

  private fun FileSpec.writeToAndClose(appendable: Appendable) {
    writeTo(appendable)
    if (appendable is Closeable) appendable.close()
  }
}

internal fun <T> tryWithElement(
  element: PsiElement,
  block: () -> T
): T {
  try {
    return block()
  } catch (e: Throwable) {
    val exception = IllegalStateException("Failed to compile $element :\n${element.text}")
    exception.initCause(e)
    throw exception
  }
}
