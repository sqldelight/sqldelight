package app.cash.sqldelight.intellij.intentions

import com.alecstrong.sql.psi.core.psi.SqlColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType

class QualifyColumnNameIntention : BaseElementAtCaretIntentionAction() {

  override fun getFamilyName(): String {
    return INTENTIONS_FAMILY_NAME_REFACTORINGS
  }

  override fun getText(): String {
    return "Qualify identifier"
  }

  override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
    val columnName = element.parentOfType<SqlColumnName>(true) ?: return false
    return columnName.parent is SqlColumnExpr && columnName.prevSibling.elementType != SqlTypes.DOT
  }

  override fun invoke(project: Project, editor: Editor, element: PsiElement) {
    val columnName = element.parentOfType<SqlColumnName>(true) ?: return
    val columnText = columnName.text
    val tableNamesOrAliases = columnName.queryAvailable(columnName)
      .filter { result -> result.table != null }
      .flatMap { result ->
        result.columns.filter { column -> column.element.textMatches(columnText) }
          .map { column -> column.element to result.table!! }
      }
      .groupBy({ it.first }, { it.second })
      .values
      .flatten()
      .map { it.text }
      .distinct()

    if (tableNamesOrAliases.isEmpty()) {
      return
    }

    val document = editor.document
    val columnRange = document.createRangeMarker(columnName.textRange)

    val callback = { qualifier: String ->
      WriteCommandAction.runWriteCommandAction(project) {
        document.replaceString(
          columnRange.startOffset, columnRange.endOffset, "$qualifier.$columnText"
        )
      }
    }
    if (tableNamesOrAliases.size == 1) {
      callback(tableNamesOrAliases.first())
    } else {
      JBPopupFactory.getInstance()
        .createPopupChooserBuilder(tableNamesOrAliases)
        .setMovable(true)
        .setResizable(true)
        .setItemChosenCallback(callback)
        .createPopup()
        .showInBestPositionFor(editor)
    }
  }
}
