package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.Queryable
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.SqlCompoundSelectStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateViewStmt
import com.alecstrong.sql.psi.core.psi.SqlCteTableName
import com.alecstrong.sql.psi.core.psi.SqlViewName
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType

class SelectQueryable(
  override val select: SqlCompoundSelectStmt,
  override var statement: SqlAnnotatedElement = select,
) : QueryWithResults {

  /**
   * If this query is a pure select from a table (virtual or otherwise), this returns the LazyQuery
   * which points to that table (Pure meaning it has exactly the same columns in the same order).
   */
  override val pureTable: NamedElement? by lazy {
    val pureColumns = select.queryExposed().singleOrNull()?.columns?.flattenCompounded()

    // First check to see if its just the table we're observing directly.
    val tablesSelected = select.selectStmtList.flatMap {
      it.joinClause?.tableOrSubqueryList?.mapNotNull { tableOrSubquery ->
        val resolvedTable = tableOrSubquery.tableName?.reference?.resolve() ?: return@mapNotNull null
        PsiTreeUtil.getParentOfType(resolvedTable, Queryable::class.java)?.tableExposed()
      }.orEmpty()
    }
    tablesSelected.forEach {
      if (it.query.columns.flattenCompounded() == pureColumns) {
        val table = it.query.table
        if (table is SqlViewName) {
          // check, if this view uses exactly 1 pure table and use this table, if found.
          val createViewStmt = table.nameIdentifier?.parentOfType<SqlCreateViewStmt>()?.compoundSelectStmt
          if (createViewStmt != null) {
            val foundPureTable = SelectQueryable(createViewStmt).pureTable
            if (foundPureTable != null) {
              return@lazy foundPureTable
            }
          }
        }
        return@lazy it.tableName
      }
    }

    return@lazy select.tablesAvailable(select).firstOrNull {
      (it.tableName.parent !is SqlCteTableName) &&
        it.query.columns.flattenCompounded() == pureColumns
    }?.tableName
  }
}
