package app.cash.sqldelight.dialects.sqlite_3_24

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_24.grammar.psi.SqliteUpsertDoUpdate
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlSetterExpression
import com.intellij.psi.PsiElement
import app.cash.sqldelight.dialects.sqlite_3_18.SqliteTypeResolver as Sqlite318TypeResolver

class SqliteTypeResolver(parentResolver: TypeResolver) : Sqlite318TypeResolver(parentResolver) {
  override fun argumentType(
    parent: PsiElement,
    argument: SqlExpr
  ): IntermediateType {
    when (parent) {
      is SqlSetterExpression -> when (parent.parent!!) {
        is SqliteUpsertDoUpdate -> return resolvedType(parent.expr)
      }
    }
    return super.argumentType(parent, argument)
  }
}
