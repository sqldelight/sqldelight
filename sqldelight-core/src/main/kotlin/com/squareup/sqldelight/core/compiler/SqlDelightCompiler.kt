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

import com.intellij.openapi.project.Project
import com.squareup.kotlinpoet.FileSpec
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.lang.SqlDelightFile

private typealias FileAppender = (fileName: String) -> Appendable

object SqlDelightCompiler {
  fun compile(file: SqlDelightFile, output: FileAppender) {
    writeTableInterfaces(file, output)
    writeViewInterfaces(file, output)
    writeQueryInterfaces(file, output)
  }

  fun writeDatabaseFile(project: Project, output: FileAppender) {
    val packageName = SqlDelightFileIndex.getInstance(project).packageName
    val outputDirectory = packageName.replace(".", "/")
    val generator = DatabaseGenerator(project)
    val databaseType = generator.databaseSpec()
    FileSpec.builder(packageName, databaseType.name!!)
        .apply {
          addType(databaseType)
        }
        .build()
        .writeTo(output("$outputDirectory/${databaseType.name}.kt"))
  }

  internal fun writeTableInterfaces(file: SqlDelightFile, output: FileAppender) {
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
              .writeTo(output("${file.generatedDir}/${createTable.tableName.name.capitalize()}.kt"))
        }
  }

  internal fun writeViewInterfaces(file: SqlDelightFile, output: FileAppender) {
    file.sqliteStatements()
        .mapNotNull { it.statement.createViewStmt }
        .map { NamedQuery(it.viewName.name, it.compoundSelectStmt) }
        .writeQueryInterfaces(file, output)
  }

  internal fun writeQueryInterfaces(file: SqlDelightFile, output: FileAppender) {
    file.sqliteStatements()
        .mapNotNull {
          it.identifier?.name?.let { name ->
            val query = it.statement.compoundSelectStmt ?: return@mapNotNull null
            return@mapNotNull NamedQuery(name, query)
          }
          return@mapNotNull null
        }
        .writeQueryInterfaces(file, output)
  }

  private fun List<NamedQuery>.writeQueryInterfaces(file: SqlDelightFile, output: FileAppender) {
    filter { it.select.queryExposed().singleOrNull() !in it.select.tablesAvailable(it.select).map { it.query() } }
        .forEach { namedQuery ->
          FileSpec.builder(file.packageName, namedQuery.name)
              .apply {
                val generator = QueryInterfaceGenerator(namedQuery)
                addType(generator.kotlinInterfaceSpec())
                addType(generator.interfaceSpec())
              }
              .build()
              .writeTo(output("${file.generatedDir}/${namedQuery.name.capitalize()}.kt"))
        }
  }
}
