package com.squareup.sqldelight.core.lang.util

import com.alecstrong.sqlite.psi.core.psi.SqliteCommonTableExpression
import com.alecstrong.sqlite.psi.core.psi.SqliteCompoundSelectStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteCreateViewStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteTableAlias
import com.alecstrong.sqlite.psi.core.psi.SqliteTableName
import com.alecstrong.sqlite.psi.core.psi.SqliteViewName
import com.intellij.psi.PsiElement

internal fun SqliteCompoundSelectStmt.tablesObserved() = findChildrenOfType<SqliteTableName>()
    .mapNotNull { it.reference?.resolve() }
    .distinct()
    .flatMap { it.referencedTables() }
    .distinct()

internal fun PsiElement.referencedTables(): List<SqliteCreateTableStmt> = when (this) {
  is SqliteCompoundSelectStmt -> tablesObserved()
  is SqliteTableAlias -> source().referencedTables()
  is SqliteTableName, is SqliteViewName -> {
    val parentRule = parent!!
    when (parentRule) {
      is SqliteCreateTableStmt -> listOf(parentRule)
      is SqliteCreateViewStmt -> parentRule.compoundSelectStmt?.tablesObserved() ?: emptyList()
      is SqliteCommonTableExpression -> parentRule.compoundSelectStmt.tablesObserved()
      else -> reference!!.resolve()!!.referencedTables()
    }
  }
  else -> throw IllegalStateException("Cannot get reference table for psi type ${this.javaClass}")
}