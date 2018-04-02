package com.squareup.sqldelight.core.lang.util

import com.alecstrong.sqlite.psi.core.psi.SqliteCompoundSelectStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteCreateViewStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteCreateVirtualTableStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteCteTableName
import com.alecstrong.sqlite.psi.core.psi.SqliteTableAlias
import com.alecstrong.sqlite.psi.core.psi.SqliteTableName
import com.alecstrong.sqlite.psi.core.psi.SqliteViewName
import com.alecstrong.sqlite.psi.core.psi.SqliteWithClause
import com.intellij.psi.PsiElement

internal fun SqliteCompoundSelectStmt.tablesObserved() = findChildrenOfType<SqliteTableName>()
    .mapNotNull { it.reference?.resolve() }
    .distinct()
    .flatMap { it.referencedTables(this) }
    .distinct()

internal fun PsiElement.referencedTables(
  compoundSelectStmt: SqliteCompoundSelectStmt? = null
): List<SqliteTableName> = when (this) {
  is SqliteCompoundSelectStmt -> tablesObserved()
  is SqliteTableAlias -> source().referencedTables()
  is SqliteTableName, is SqliteViewName -> {
    val parentRule = parent!!
    when (parentRule) {
      is SqliteCreateTableStmt -> listOf(parentRule.tableName)
      is SqliteCreateVirtualTableStmt -> listOf(parentRule.tableName)
      is SqliteCreateViewStmt -> parentRule.compoundSelectStmt?.tablesObserved() ?: emptyList()
      is SqliteCteTableName -> {
        val withClause = parentRule.parent as SqliteWithClause
        val index = withClause.cteTableNameList.indexOf(parentRule)
        val withSelect = withClause.compoundSelectStmtList[index]
        if (withSelect == compoundSelectStmt) {
          // Recursive subquery. We've already resolved the other tables in this recursive query
          // so quit out.
          emptyList()
        } else {
          withClause.compoundSelectStmtList[index].tablesObserved()
        }
      }
      else -> reference!!.resolve()!!.referencedTables()
    }
  }
  else -> throw IllegalStateException("Cannot get reference table for psi type ${this.javaClass}")
}