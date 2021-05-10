package app.cash.sqldelight.intellij.refactoring

import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.refactoring.suggested.SuggestedChangeSignatureData
import com.intellij.refactoring.suggested.SuggestedRefactoringAvailability
import com.intellij.refactoring.suggested.SuggestedRefactoringData
import com.intellij.refactoring.suggested.SuggestedRefactoringState
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport

class SqlDelightSuggestedRefactoringAvailability(
  refactoringSupport: SuggestedRefactoringSupport
) : SuggestedRefactoringAvailability(refactoringSupport) {

  override fun detectAvailableRefactoring(state: SuggestedRefactoringState): SuggestedRefactoringData? {
    val oldSignature = state.oldSignature
    val newSignature = state.newSignature
    val declaration = state.declaration as SqlCreateTableStmt
    val changeSignatureData = SuggestedChangeSignatureData.create(state, "migration")
    if (hasParameterAddedRemovedOrReordered(oldSignature, newSignature)) {
      return changeSignatureData
    }
    if (hasTypeChanges(oldSignature, newSignature)) {
      // type changes not supported yet
      return null
    }
    val (_, renameData) = nameChanges(
      oldSignature = oldSignature,
      newSignature = newSignature,
      declaration = declaration,
      parameters = declaration.columnDefList.map { it.columnName }
    )
    return renameData?.let { changeSignatureData }
  }
}
