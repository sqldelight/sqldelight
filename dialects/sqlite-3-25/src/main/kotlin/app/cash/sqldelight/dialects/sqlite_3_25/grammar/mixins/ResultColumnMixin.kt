package app.cash.sqldelight.dialects.sqlite_3_25.grammar.mixins

import app.cash.sqldelight.dialects.sqlite_3_25.grammar.psi.SqliteResultColumn
import com.alecstrong.sql.psi.core.ModifiableFileLazy
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.impl.SqlResultColumnImpl
import com.intellij.lang.ASTNode

internal abstract class ResultColumnMixin(node: ASTNode) : SqlResultColumnImpl(node), SqliteResultColumn {
  private val queryExposed = ModifiableFileLazy lazy@{
    if (windowFunctionInvocation != null) {
      var column = QueryElement.QueryColumn(this)
      columnAlias?.let { alias ->
        column = column.copy(element = alias)
      }

      return@lazy listOf(QueryResult(columns = listOf(column)))
    }

    return@lazy super.queryExposed()
  }

  override fun queryExposed() = queryExposed.forFile(containingFile)
}
