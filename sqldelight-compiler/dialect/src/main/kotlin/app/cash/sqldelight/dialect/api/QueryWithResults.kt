package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement

interface QueryWithResults {
  val statement: SqlAnnotatedElement
  val select: QueryElement
  val pureTable: NamedElement?
}
