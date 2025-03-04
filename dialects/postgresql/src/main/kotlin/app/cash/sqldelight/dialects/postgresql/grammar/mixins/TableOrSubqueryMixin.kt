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
        val tableFunctionAliasName = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionTableAlias>().single()
        val aliasColumns: List<PostgreSqlTableFunctionColumnAlias> = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionColumnAlias>()
        return@lazy listOf(
          QueryResult(
            table = tableFunctionAliasName,
            columns = aliasColumns.map { QueryElement.QueryColumn(it) },
          ),
        )
      }
      return@lazy listOf(
        QueryResult(
          table = unnestTableFunction,
          columns = children.filterIsInstance<SqlExpr>().map { QueryElement.QueryColumn(unnestTableFunction!!) },
        ),
      )
    }
    super.queryExposed()
  }
  override fun tablesAvailable(child: PsiElement): Collection<LazyQuery> {
    if (unnestTableFunction != null) {
      val tableFunctionAlias = children.filterIsInstance<PostgreSqlTableFunctionAliasName>().single()
      val tableName = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionTableAlias>().single()
      return super.tablesAvailable(child) + LazyQuery(tableName) {
        QueryResult(
          tableName,
          emptyList(),
        )
      }
    } else {
      return super.tablesAvailable(child)
    }
  }

  override fun queryExposed() = queryExposed.forFile(containingFile)

  override fun queryAvailable(child: PsiElement): Collection<QueryResult> {
    if (child is SqlColumnName || child is PostgreSqlTableFunctionAliasName) {
      return tablesAvailable(child).map {
        val tableFunctionAlias = children.filterIsInstance<PostgreSqlTableFunctionAliasName>().single()
        val tableName = tableFunctionAlias.children.filterIsInstance<PostgreSqlTableFunctionTableAlias>().single()
        QueryResult(
          tableName,
          it.query.columns,
        )
      }
    }
    if (child is SqlExpr) {
      val parent = parent as SqlJoinClause
      return parent.tableOrSubqueryList.takeWhile { it != this }.flatMap { it.queryExposed() }
    }
    return super.queryAvailable(child)
  }
}
