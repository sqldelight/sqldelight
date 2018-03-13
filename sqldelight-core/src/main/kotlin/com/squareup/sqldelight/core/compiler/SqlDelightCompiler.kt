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

import com.intellij.openapi.module.Module
import com.squareup.kotlinpoet.FileSpec
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.compiler.model.NamedQuery
import com.squareup.sqldelight.core.compiler.model.namedQueries
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.queriesName
import java.io.Closeable

private typealias FileAppender = (fileName: String) -> Appendable

object SqlDelightCompiler {
  fun compile(module: Module, file: SqlDelightFile, output: FileAppender) {
    writeTableInterfaces(module, file, output)
    writeViewInterfaces(module, file, output)
    writeQueryInterfaces(module, file, output)
    writeQueriesType(module, file, output)
  }

  fun writeQueryWrapperFile(module: Module, output: FileAppender) {
    val packageName = SqlDelightFileIndex.getInstance(module).packageName
    val outputDirectory = packageName.replace(".", "/")
    val queryWrapperType = QueryWrapperGenerator(module).type()
    FileSpec.builder(packageName, queryWrapperType.name!!)
        .addType(queryWrapperType)
        .build()
        .writeToAndClose(output("$outputDirectory/${queryWrapperType.name}.kt"))
  }

  internal fun writeTableInterfaces(module: Module, file: SqlDelightFile, output: FileAppender) {
    file.sqliteStatements()
        .mapNotNull { it.statement.createTableStmt }
        .forEach { createTable ->
          FileSpec.builder(file.packageName, createTable.tableName.name)
              .apply {
                val generator = TableInterfaceGenerator(createTable)
                addType(generator.kotlinInterfaceSpec())
                addType(generator.interfaceSpec())
              }
              .build()
              .writeToAndClose(output("${file.generatedDir}/${createTable.tableName.name.capitalize()}.kt"))
        }
  }

  internal fun writeViewInterfaces(module: Module, file: SqlDelightFile, output: FileAppender) {
    file.sqliteStatements()
        .mapNotNull { it.statement.createViewStmt }
        .filter { it.compoundSelectStmt != null }
        .map { NamedQuery(it.viewName.name, it.compoundSelectStmt!!) }
        .writeQueryInterfaces(file, output)
  }

  internal fun writeQueryInterfaces(module: Module, file: SqlDelightFile, output: FileAppender) {
    file.sqliteStatements().namedQueries()
        .writeQueryInterfaces(file, output)
  }

  internal fun writeQueriesType(module: Module, file: SqlDelightFile, output: FileAppender) {
    val packageName = file.packageName
    val outputDirectory = packageName.replace(".", "/")
    val queriesType = QueriesTypeGenerator(module, file).generateType()
    FileSpec.builder(packageName, file.queriesName.capitalize())
        .addType(queriesType)
        .build()
        .writeToAndClose(output("$outputDirectory/${queriesType.name}.kt"))
  }

  private fun List<NamedQuery>.writeQueryInterfaces(file: SqlDelightFile, output: FileAppender) {
    return filter { it.needsInterface() }
        .forEach { namedQuery ->
          FileSpec.builder(file.packageName, namedQuery.name)
              .apply {
                val generator = QueryInterfaceGenerator(namedQuery)
                addType(generator.kotlinInterfaceSpec())
                addType(generator.interfaceSpec())
              }
              .build()
              .writeToAndClose(output("${file.generatedDir}/${namedQuery.name.capitalize()}.kt"))
        }
  }

  private fun FileSpec.writeToAndClose(appendable: Appendable) {
    writeTo(appendable)
    if (appendable is Closeable) appendable.close()
  }
}
