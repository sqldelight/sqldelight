/*
 * Copyright (C) 2018 Square, Inc.
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
package com.squareup.sqldelight.core.compiler.model

import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteDeleteStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteInsertStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteTableName
import com.alecstrong.sqlite.psi.core.psi.SqliteUpdateStmt
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.compiler.model.NamedMutator.Delete
import com.squareup.sqldelight.core.compiler.model.NamedMutator.Insert
import com.squareup.sqldelight.core.compiler.model.NamedMutator.Update
import com.squareup.sqldelight.core.lang.SqlDelightFile.LabeledStatement
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.referencedTables

sealed class NamedMutator(
  val name: String,
  val statement: PsiElement
) : BindableQuery(statement) {
  internal val tableEffected: SqliteCreateTableStmt by lazy {
    statement.findChildrenOfType<SqliteTableName>().single().referencedTables().single()
  }

  class Insert(name: String, insert: SqliteInsertStmt) : NamedMutator(name, insert)
  class Delete(name: String, delete: SqliteDeleteStmt) : NamedMutator(name, delete)
  class Update(name: String, update: SqliteUpdateStmt) : NamedMutator(name, update)
}

internal fun Collection<LabeledStatement>.namedMutators(): List<NamedMutator> {
  return filter { it.identifier.name != null }
      .mapNotNull {
        when {
          it.statement.deleteStmt != null -> Delete(it.identifier.name!!, it.statement.deleteStmt!!)
          it.statement.insertStmt != null -> Insert(it.identifier.name!!, it.statement.insertStmt!!)
          it.statement.updateStmt != null -> Update(it.identifier.name!!, it.statement.updateStmt!!)
          else -> null
        }
  }
}