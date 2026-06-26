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

    if (generateSeriesTableFunction != null) {
      // generate_series exposes a single output column. Three aliasing forms:
      //  - `AS s(a)`  : table `s`, column named `a`        e.g. `SELECT s.a FROM generate_series(0, 14, 7) AS s(a)`
      //  - `AS s`     : the table alias renames the column e.g. `SELECT s FROM generate_series(1, 2, 10) AS s`
      //  - no alias   : the column is named `generate_series`
      val tableFunctionAlias = children.filterIsInstance<PostgreSqlTableFunctionAliasName>().firstOrNull()

      if (tableFunctionAlias != null) {
        val tableName = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionTableAlias>().single()
        val aliasColumns = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionColumnAlias>()
        val column: PsiElement = aliasColumns.singleOrNull() ?: tableName
        return@lazy listOf(
          QueryResult(
            table = tableName,
            columns = listOf(QueryElement.QueryColumn(column)),
          ),
        )
      }

      return@lazy listOf(
        QueryResult(
          table = generateSeriesTableFunction!!,
          columns = listOf(QueryElement.QueryColumn(generateSeriesTableFunction!!)),
        ),
      )
    }

    if (jsonTableFunction != null) {
      // JSON set-returning functions. `*_object_keys` may be un-aliased (column named after the
      // function); `*_array_elements`/`*_each` require column aliases (enforced in JsonTableFunctionMixin).
      val tableFunctionAlias = children.filterIsInstance<PostgreSqlTableFunctionAliasName>().firstOrNull()

      if (tableFunctionAlias != null) {
        val tableName = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionTableAlias>().single()
        val aliasColumns = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionColumnAlias>()
        val columns =
          if (aliasColumns.isNotEmpty()) aliasColumns.map { QueryElement.QueryColumn(it) } else listOf(QueryElement.QueryColumn(tableName))
        return@lazy listOf(QueryResult(table = tableName, columns = columns))
      }

      return@lazy listOf(
        QueryResult(
          table = jsonTableFunction!!,
          columns = listOf(QueryElement.QueryColumn(jsonTableFunction!!)),
        ),
      )
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

    if (generateSeriesTableFunction != null) {
      val tableFunctionAlias = children.filterIsInstance<PostgreSqlTableFunctionAliasName>().firstOrNull()

      if (tableFunctionAlias != null) {
        val tableName = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionTableAlias>().single()
        val aliasColumns = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionColumnAlias>()
        val column: PsiElement = aliasColumns.singleOrNull() ?: tableName
        return super.tablesAvailable(child) + LazyQuery(tableName) {
          QueryResult(
            table = tableName,
            columns = listOf(QueryElement.QueryColumn(column)),
          )
        }
      }

      return super.tablesAvailable(child) + LazyQuery(generateSeriesTableFunction!!) {
        QueryResult(
          table = generateSeriesTableFunction!!,
          columns = listOf(QueryElement.QueryColumn(generateSeriesTableFunction!!)),
        )
      }
    }

    if (jsonTableFunction != null) {
      val tableFunctionAlias = children.filterIsInstance<PostgreSqlTableFunctionAliasName>().firstOrNull()

      if (tableFunctionAlias != null) {
        val tableName = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionTableAlias>().single()
        val aliasColumns = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionColumnAlias>()
        val columns =
          if (aliasColumns.isNotEmpty()) aliasColumns.map { QueryElement.QueryColumn(it) } else listOf(QueryElement.QueryColumn(tableName))
        return super.tablesAvailable(child) + LazyQuery(tableName) {
          QueryResult(table = tableName, columns = columns)
        }
      }

      return super.tablesAvailable(child) + LazyQuery(jsonTableFunction!!) {
        QueryResult(
          table = jsonTableFunction!!,
          columns = listOf(QueryElement.QueryColumn(jsonTableFunction!!)),
        )
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
