package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.Queryable
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.intellij.psi.util.PsiTreeUtil

/**
 * Query deriving from the `RETURNING` clause of an expression.
 *
 * Typical use cases include `INSERT`, `UPDATE`, and `DELETE`. This class is similar to [SelectQueryable] but differs in
 * the fact that only 1 table can be part of the query, and that table is guaranteed to be "real".
 *
 * @param statement Parent statement. Typically, this is the `INSERT`, `UPDATE`, or `DELETE` statement.
 * @param select The `RETURNING` clause of the statement. Represented as a query since it returns values to the caller.
 * @param tableName Name of the table the [statement] is operating on.
 */
class ReturningQueryable(
  override var statement: SqlAnnotatedElement,
  override val select: QueryElement,
  private val tableName: SqlTableName?,
) : QueryWithResults {

  override val pureTable by lazy {
    val pureColumns = select.queryExposed().singleOrNull()?.columns?.flattenCompounded()
    val resolvedTable = tableName?.reference?.resolve()
    val table = PsiTreeUtil.getParentOfType(resolvedTable, Queryable::class.java)?.tableExposed()
      ?: return@lazy null
    val requestedColumnsAreIdenticalToTable = table.query.columns.flattenCompounded() == pureColumns
    if (requestedColumnsAreIdenticalToTable) {
      table.tableName
    } else {
      null
    }
  }
}
