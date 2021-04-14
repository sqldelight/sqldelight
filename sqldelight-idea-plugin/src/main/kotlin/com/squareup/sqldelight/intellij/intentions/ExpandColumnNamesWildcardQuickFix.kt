package com.squareup.sqldelight.intellij.intentions

import com.alecstrong.sql.psi.core.psi.SqlResultColumn
import com.alecstrong.sql.psi.core.psi.SqlSelectStmt
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ReadOnlyModificationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.intellij.util.findElementOfTypeAtOffset
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ExpandColumnNamesWildcardQuickFix : BaseIntentionAction() {

  override fun getFamilyName() = INTENTIONS_FAMILY_NAME_REFACTORINGS

  override fun getText() = INTENTION_EXPAND_COLUMN_NAMES_TEXT

  override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
    val caret = editor.caretModel.offset
    return selectStatementAtCaretWithColumnNamesWildcard(
      file = file as? SqlDelightFile ?: return false,
      caret = caret
    ) != null
  }

  private fun selectStatementAtCaretWithColumnNamesWildcard(
    file: SqlDelightFile,
    caret: Int
  ): SqlSelectStmt? {
    val selectStatement = file.findElementOfTypeAtOffset<SqlSelectStmt>(caret) ?: return null
    val resultColumns = selectStatement.resultColumnList
    return if (resultColumns.any { it.textMatches("*") }) selectStatement else null
  }

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    WriteCommandAction.writeCommandAction(project).run<ReadOnlyModificationException> {
      val caret = editor.caretModel.offset
      selectStatementAtCaretWithColumnNamesWildcard(file as SqlDelightFile, caret)?.run {
        val allColumns = queryExposed()
          .flatMap { it.columns }
          .distinctBy { it.element.text }
          .joinToString(separator = ", ") { it.element.text }
        val (start, end) = resultColumnList.startEndOffset()
        editor.document.replaceString(start, end, allColumns)
      }
    }
  }

  private fun List<SqlResultColumn>.startEndOffset(): Pair<Int, Int> {
    var start = firstOrNull()?.startOffset ?: 0
    var end = firstOrNull()?.endOffset ?: 0
    for (i in 1..lastIndex) {
      start = minOf(start, get(i).startOffset)
      end = maxOf(end, get(i).endOffset)
    }
    return start to end
  }
}
