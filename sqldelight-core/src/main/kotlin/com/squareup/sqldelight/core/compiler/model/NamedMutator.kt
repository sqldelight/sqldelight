package com.squareup.sqldelight.core.compiler.model

import com.alecstrong.sqlite.psi.core.psi.SqliteBindExpr
import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteDeleteStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteInsertStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteTableName
import com.alecstrong.sqlite.psi.core.psi.SqliteUpdateStmt
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.compiler.model.NamedMutator.Delete
import com.squareup.sqldelight.core.compiler.model.NamedMutator.Insert
import com.squareup.sqldelight.core.compiler.model.NamedMutator.Update
import com.squareup.sqldelight.core.lang.IntermediateType
import com.squareup.sqldelight.core.lang.SqlDelightFile.LabeledStatement
import com.squareup.sqldelight.core.lang.util.argumentType
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.util.referencedTables

sealed class NamedMutator(val name: String, val statement: PsiElement) {
  /**
   * The collection of all bind expressions in this query.
   */
  internal val arguments: List<IntermediateType> by lazy {
    statement.findChildrenOfType<SqliteBindExpr>().map { it.argumentType() }
  }

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