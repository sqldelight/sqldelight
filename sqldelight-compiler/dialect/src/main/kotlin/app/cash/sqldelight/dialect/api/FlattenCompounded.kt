package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.psi.QueryElement

internal fun List<QueryElement.QueryColumn>.flattenCompounded(): List<QueryElement.QueryColumn> {
  return map { column ->
    if (column.compounded.none { it.element != column.element || it.nullable != column.nullable }) {
      column.copy(compounded = emptyList())
    } else {
      column
    }
  }
}
