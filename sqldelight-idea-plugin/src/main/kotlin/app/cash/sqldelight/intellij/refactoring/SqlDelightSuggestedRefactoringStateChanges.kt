package app.cash.sqldelight.intellij.refactoring

import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.SuggestedRefactoringState
import com.intellij.refactoring.suggested.SuggestedRefactoringStateChanges
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.ParameterAdditionalData

class SqlDelightSuggestedRefactoringStateChanges(
  refactoringSupport: SuggestedRefactoringSupport
) : SuggestedRefactoringStateChanges(refactoringSupport) {

  override fun parameterMarkerRanges(declaration: PsiElement): List<TextRange?> {
    return (declaration as SqlCreateTableStmt).columnDefList.map { it.textRange }
  }

  override fun signature(
    declaration: PsiElement,
    prevState: SuggestedRefactoringState?
  ): SuggestedRefactoringSupport.Signature? {
    declaration as SqlCreateTableStmt

    val tableName = declaration.tableName.name
    val columns = declaration.columnDefList.map { columnDef ->
      SuggestedRefactoringSupport.Parameter(
        id = Any(),
        name = columnDef.columnName.name,
        type = columnDef.columnType.typeName.text,
        additionalData = columnDef.columnConstraintList.takeIf { it.isNotEmpty() }?.let { list ->
          ColumnConstraints((list.filter { it.text.isNotBlank() }.joinToString(" ") { it.text.trim() }))
        }
      )
    }
    val signature = SuggestedRefactoringSupport.Signature.create(
      name = tableName,
      type = null,
      parameters = columns,
      additionalData = null // TODO: support table constraints
    ) ?: return null

    return if (prevState == null) signature else matchParametersWithPrevState(
      signature, declaration, prevState
    )
  }

  data class ColumnConstraints(val constraints: String) : ParameterAdditionalData {
    override fun toString(): String {
      return constraints
    }
  }
}
