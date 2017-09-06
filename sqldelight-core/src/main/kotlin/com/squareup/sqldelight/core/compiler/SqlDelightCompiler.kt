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

import com.squareup.kotlinpoet.KotlinFile
import com.squareup.sqldelight.core.lang.SqlDelightFile

private typealias FileAppender = (fileName: String) -> Appendable

object SqlDelightCompiler {
  fun compile(file: SqlDelightFile) {
    TODO("Call write functions to output to appropriate files")
  }

  internal fun writeTableInterfaces(file: SqlDelightFile, output: FileAppender) {
    file.sqliteStatements()
        .mapNotNull { it.statement.createTableStmt }
        .forEach { createTable ->
          KotlinFile.builder(file.packageName, createTable.tableName.name)
              .apply {
                val generator = TableInterfaceGenerator(createTable)
                addType(generator.interfaceSpec())
                addType(generator.kotlinInterfaceSpec())
              }
              .build()
              .writeTo(output("${createTable.tableName.name.capitalize()}.kt"))
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
          if (it.name == null) return@mapNotNull null
          val query = it.statement.compoundSelectStmt ?: return@mapNotNull null
          return@mapNotNull NamedQuery(it.name, query)
        }
        .writeQueryInterfaces(file, output)
  }

  private fun List<NamedQuery>.writeQueryInterfaces(file: SqlDelightFile, output: FileAppender) {
    filter { it.select.queryExposed().singleOrNull() !in it.select.tablesAvailable(it.select).map { it.query() } }
        .forEach { namedQuery ->
          KotlinFile.builder(file.packageName, namedQuery.name)
              .apply {
                val generator = QueryInterfaceGenerator(namedQuery)
                addType(generator.interfaceSpec())
                addType(generator.kotlinInterfaceSpec())
              }
              .build()
              .writeTo(output("${namedQuery.name.capitalize()}.kt"))
        }
  }
}
