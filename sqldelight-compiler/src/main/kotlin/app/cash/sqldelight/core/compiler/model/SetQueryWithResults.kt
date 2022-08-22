package app.cash.sqldelight.core.compiler.model

import app.cash.sqldelight.dialect.api.QueryWithResults
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.SqlSetStmt

class SetQueryWithResults(setStmt: SqlSetStmt) : QueryWithResults {
  override var statement: SqlAnnotatedElement = setStmt
  override val select: SqlSetStmt = setStmt
  override val pureTable: NamedElement? = null
}
