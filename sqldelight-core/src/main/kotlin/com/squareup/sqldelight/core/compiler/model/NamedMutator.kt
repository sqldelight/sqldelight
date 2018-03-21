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
import com.alecstrong.sqlite.psi.core.psi.SqliteDeleteStmtLimited
import com.alecstrong.sqlite.psi.core.psi.SqliteInsertStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteTableName
import com.alecstrong.sqlite.psi.core.psi.SqliteUpdateStmtLimited
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.compiler.model.NamedMutator.Delete
import com.squareup.sqldelight.core.compiler.model.NamedMutator.Insert
import com.squareup.sqldelight.core.compiler.model.NamedMutator.Update
import com.squareup.sqldelight.core.lang.SqlDelightFile.LabeledStatement
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.referencedTables

sealed class NamedMutator(
  statement: PsiElement,
  identifier: StmtIdentifierMixin
) : BindableQuery(identifier, statement) {
  val name = identifier.name!!
  internal val tableEffected: SqliteCreateTableStmt by lazy {
    statement.findChildrenOfType<SqliteTableName>().single().referencedTables().single()
  }

  class Insert(insert: SqliteInsertStmt, identifier: StmtIdentifierMixin) : NamedMutator(insert, identifier)
  class Delete(delete: SqliteDeleteStmtLimited, identifier: StmtIdentifierMixin) : NamedMutator(delete, identifier)
  class Update(update: SqliteUpdateStmtLimited, identifier: StmtIdentifierMixin) : NamedMutator(update, identifier)
}

internal fun Collection<LabeledStatement>.namedMutators(): List<NamedMutator> {
  return filter { it.identifier.name != null }
      .mapNotNull {
        when {
          it.statement.deleteStmtLimited != null -> Delete(it.statement.deleteStmtLimited!!, it.identifier)
          it.statement.insertStmt != null -> Insert(it.statement.insertStmt!!, it.identifier)
          it.statement.updateStmtLimited != null -> Update(it.statement.updateStmtLimited!!, it.identifier)
          else -> null
        }
  }
}