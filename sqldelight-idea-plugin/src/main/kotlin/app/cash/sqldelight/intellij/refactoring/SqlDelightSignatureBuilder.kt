package app.cash.sqldelight.intellij.refactoring

import app.cash.sqldelight.core.lang.psi.asParameter
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Signature

internal class SqlDelightSignatureBuilder {
  fun signature(declaration: SqlCreateTableStmt): Signature? {
    val tableName = declaration.tableName.name
    val columns = declaration.columnDefList.map { columnDef -> columnDef.asParameter() }

    return Signature.create(
      name = tableName,
      type = null,
      parameters = columns,
      additionalData = null // TODO: support table constraints
    )
  }
}
