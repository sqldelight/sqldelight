package app.cash.sqldelight.core.compiler.model

import app.cash.sqldelight.core.lang.psi.StmtIdentifierMixin
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement

open class NamedExecute(
  identifier: StmtIdentifierMixin,
  statement: SqlAnnotatedElement
) : BindableQuery(identifier, statement) {
  val name = identifier.name!!

  override val id: Int
    // the sqlFile package name -> com.example.
    // sqlFile.name -> test.sq
    // name -> query name
    get() = idForIndex(null)
}
