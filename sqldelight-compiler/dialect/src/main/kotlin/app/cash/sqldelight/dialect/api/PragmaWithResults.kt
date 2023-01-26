package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.SqlPragmaStmt
import com.alecstrong.sql.psi.core.psi.impl.SqlPragmaNameImpl
import com.intellij.lang.ASTNode

class PragmaWithResults(pragmaStmt: SqlPragmaStmt) : QueryWithResults {
  override var statement: SqlAnnotatedElement = pragmaStmt
  override val select: QueryElement = pragmaStmt.pragmaName as SqlDelightPragmaName
  override val pureTable: NamedElement? = null
}

class SqlDelightPragmaName(node: ASTNode?) : SqlPragmaNameImpl(node), QueryElement {
  override fun queryExposed() = listOf(
    QueryResult(
      column = this,
    ),
  )
}
