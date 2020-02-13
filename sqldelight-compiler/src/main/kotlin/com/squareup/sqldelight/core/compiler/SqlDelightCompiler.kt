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
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.compiler.model.NamedQuery
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.queriesName
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import java.io.Closeable

private typealias FileAppender = (fileName: String) -> Appendable

object SqlDelightCompiler {
  fun writeInterfaces(
    module: Module,
    file: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender
  ) {
    writeTableInterfaces(module, file, implementationFolder, output)
    writeViewInterfaces(module, file, implementationFolder, output)
    writeQueryInterfaces(module, file, implementationFolder, output)
    writeQueriesInterface(module, file, implementationFolder, output)
    writeQueryWrapperInterface(module, file, implementationFolder, output)
  }

  fun writeImplementations(
    module: Module,
    sourceFile: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender
  ) {
    val fileIndex = SqlDelightFileIndex.getInstance(module)
    val packageName = "${fileIndex.packageName}.$implementationFolder"
    val outputDirectory = "${fileIndex.outputDirectory}/${packageName.replace(".", "/")}"
    val databaseImplementationType = QueryWrapperGenerator(module, sourceFile).type(packageName)
    val exposer = DatabaseExposerGenerator(databaseImplementationType, fileIndex)

    FileSpec.builder(packageName, databaseImplementationType.name!!)
        .addProperty(exposer.exposedSchema())
        .addFunction(exposer.exposedConstructor())
        .addType(databaseImplementationType)
        .apply {
          fileIndex.sourceFolders(sourceFile, includeDependencies = true)
              .flatMap { it.findChildrenOfType<SqlDelightFile>() }
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
    val queryWrapperType = QueryWrapperGenerator(module, sourceFile).interfaceType()
    FileSpec.builder(packageName, queryWrapperType.name!!)
        // TODO: Remove these when kotlinpoet supports top level types.
        .addImport("$packageName.$implementationFolder", "newInstance", "schema")
        .apply {
          var index = 0
          fileIndex.dependencies.forEach { (packageName, className) ->
            addAliasedImport(ClassName(packageName, className), "$className${index++}")
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
    output: FileAppender
  ) {
    file.sqliteStatements()
        .mapNotNull { it.statement.createTableStmt }
        .forEach { createTable ->
          FileSpec.builder(file.packageName, allocateName(createTable.tableName))
              .apply {
                tryWithElement(createTable) {
                  val generator = TableInterfaceGenerator(createTable)
                  addType(generator.kotlinInterfaceSpec())
                }
              }
              .build()
              .writeToAndClose(output("${file.generatedDir}/${allocateName(createTable.tableName).capitalize()}.kt"))
        }
  }

  internal fun writeViewInterfaces(
    module: Module,
    file: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender
  ) {
    file.sqliteStatements()
        .mapNotNull { it.statement.createViewStmt }
        .filter { it.compoundSelectStmt != null }
        .map { NamedQuery(allocateName(it.viewName), it.compoundSelectStmt!!, it.viewName) }
        .writeQueryInterfaces(file, output)
  }

  internal fun writeQueryInterfaces(
    module: Module,
    file: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender
  ) {
    file.namedQueries.writeQueryInterfaces(file, output)
  }

  internal fun writeQueriesInterface(
    module: Module,
    file: SqlDelightFile,
    implementationFolder: String,
    output: FileAppender
  ) {
    val packageName = file.packageName
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
    return filter { tryWithElement(it.select) { it.needsInterface() } }
        .forEach { namedQuery ->
          FileSpec.builder(file.packageName, namedQuery.name)
              .apply {
                tryWithElement(namedQuery.select) {
                  val generator = QueryInterfaceGenerator(namedQuery)
                  addType(generator.kotlinInterfaceSpec())
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
) : T {
  try {
    return block()
  } catch (e: Throwable) {
    val exception = IllegalStateException("Failed to compile $element :\n${element.text}")
    exception.initCause(e)
    throw exception
  }
}
