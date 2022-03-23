package app.cash.sqldelight.core.compiler.model

import app.cash.sqldelight.core.lang.psi.StmtIdentifierMixin
import app.cash.sqldelight.core.lang.util.sqFile
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

  internal fun idForIndex(index: Int?): Int {
    val postFix = if (index == null) "" else "_$index"
    return getUniqueQueryIdentifier(
      statement.sqFile().let {
        "${it.packageName}:${it.name}:$name$postFix"
      }
    )
  }
}
