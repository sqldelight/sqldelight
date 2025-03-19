package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableFunctionAliasName
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableFunctionColumnAlias
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableFunctionTableAlias
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableOrSubquery
import com.alecstrong.sql.psi.core.ModifiableFileLazy
import com.alecstrong.sql.psi.core.psi.LazyQuery
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlJoinClause
import com.alecstrong.sql.psi.core.psi.impl.SqlTableOrSubqueryImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

internal abstract class TableOrSubqueryMixin(node: ASTNode) :
  SqlTableOrSubqueryImpl(node),
  PostgreSqlTableOrSubquery {

  private val queryExposed = ModifiableFileLazy lazy@{
    if (unnestTableFunction != null) {
      val tableFunctionAlias = children.filterIsInstance<PostgreSqlTableFunctionAliasName>().firstOrNull()

      if (tableFunctionAlias != null) {
        // Case with AS alias(columns) - e.g., UNNEST(business.locations) AS loc(zip)
        val tableFunctionAliasName = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionTableAlias>().single()
        val aliasColumns = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionColumnAlias>()

        return@lazy listOf(
          QueryResult(
            table = tableFunctionAliasName,
            columns = aliasColumns.map { QueryElement.QueryColumn(it) },
          ),
        )
      } else {
        // Case without column aliases - e.g., UNNEST(business.locations) AS r
        return@lazy listOf(
          QueryResult(
            table = unnestTableFunction,
            columns = listOf(QueryElement.QueryColumn(unnestTableFunction!!)),
          ),
        )
      }
    }

    // Default to parent implementation for non-UNNEST cases
    super.queryExposed()
  }

  override fun queryExposed() = queryExposed.forFile(containingFile)

  override fun tablesAvailable(child: PsiElement): Collection<LazyQuery> {
    if (unnestTableFunction != null) {
      val tableFunctionAlias = children.filterIsInstance<PostgreSqlTableFunctionAliasName>().firstOrNull()

      if (tableFunctionAlias != null) {
        val tableName = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionTableAlias>().single()
        val aliasColumns = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionColumnAlias>()

        // Include both parent tables and the UNNEST table
        return super.tablesAvailable(child) + LazyQuery(tableName) {
          QueryResult(
            table = tableName,
            columns = aliasColumns.map { QueryElement.QueryColumn(it) },
          )
        }
      } else {
        // Handle case when UNNEST is used without column aliases
        return super.tablesAvailable(child) + LazyQuery(unnestTableFunction!!) {
          QueryResult(
            table = unnestTableFunction!!,
            columns = listOf(QueryElement.QueryColumn(unnestTableFunction!!)),
          )
        }
      }
    }

    return super.tablesAvailable(child)
  }

  override fun queryAvailable(child: PsiElement): Collection<QueryResult> {
    // For column references within the UNNEST clause
    if (child is SqlColumnName) {
      // Return both the UNNEST table and any tables from outer scopes
      return tablesAvailable(child).map { it.query }
    }

    // For table alias references
    if (child is PostgreSqlTableFunctionAliasName || child is PostgreSqlTableFunctionTableAlias) {
      return tablesAvailable(child).map { it.query }
    }

    // For expressions within the UNNEST clause
    if (child is SqlExpr) {
      val parent = parent

      // Handle expressions in JOIN clauses
      if (parent is SqlJoinClause) {
        // In a JOIN, tables mentioned earlier are available to later parts
        val availableTables = parent.tableOrSubqueryList.takeWhile { it != this }
        if (availableTables.isNotEmpty()) {
          return availableTables.flatMap { it.queryExposed() }
        }
      }

      // Include tables from outer scopes for subqueries
      return super.queryAvailable(child)
    }

    return super.queryAvailable(child)
  }
}
