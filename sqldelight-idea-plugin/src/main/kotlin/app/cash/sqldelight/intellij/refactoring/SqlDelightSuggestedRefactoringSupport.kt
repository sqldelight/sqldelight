package app.cash.sqldelight.intellij.refactoring

import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.core.psi.SqlDelightImportStmtList
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.SuggestedRefactoringAvailability
import com.intellij.refactoring.suggested.SuggestedRefactoringExecution
import com.intellij.refactoring.suggested.SuggestedRefactoringStateChanges
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport
import com.intellij.refactoring.suggested.SuggestedRefactoringUI
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset

class SqlDelightSuggestedRefactoringSupport : SuggestedRefactoringSupport {

  override val availability: SuggestedRefactoringAvailability =
    SqlDelightSuggestedRefactoringAvailability(this)
  override val execution: SuggestedRefactoringExecution =
    SqlDelightSuggestedRefactoringExecution(this)
  override val stateChanges: SuggestedRefactoringStateChanges =
    SqlDelightSuggestedRefactoringStateChanges(this)
  override val ui: SuggestedRefactoringUI =
    SqlDelightSuggestedRefactoringUI()

  override fun isDeclaration(psiElement: PsiElement): Boolean {
    return psiElement is SqlCreateTableStmt
  }

  override fun isIdentifierPart(c: Char): Boolean {
    return true
  }

  override fun isIdentifierStart(c: Char): Boolean {
    val id = Character.isLetter(c) || c == '`' || c == '['
    val string = c == '\'' || c == '"'
    return id || string
  }

  override fun nameRange(declaration: PsiElement): TextRange? {
    return (declaration as SqlCreateTableStmt).tableName.textRange
  }

  override fun signatureRange(declaration: PsiElement): TextRange {
    val createTableStmt = declaration as SqlCreateTableStmt
    val startOffset = createTableStmt.tableName.startOffset
    val endOffset = createTableStmt.endOffset
    return TextRange(startOffset, endOffset)
  }

  override fun importsRange(psiFile: PsiFile): TextRange? {
    val importStmtList = psiFile.findChildrenOfType<SqlDelightImportStmtList>()
    if (importStmtList.isEmpty()) {
      return null
    }
    val first = importStmtList.first()
    val last = importStmtList.last()
    return TextRange(first.startOffset, last.endOffset)
  }
}
