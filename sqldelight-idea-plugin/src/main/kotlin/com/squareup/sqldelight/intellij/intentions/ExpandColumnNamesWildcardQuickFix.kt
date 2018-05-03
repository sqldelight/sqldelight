package com.squareup.sqldelight.intellij.intentions

import com.alecstrong.sqlite.psi.core.psi.SqliteSelectStmt
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
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
    return selectStatementAtCaretWithColumnNamesWildcard(file as SqlDelightFile, caret) != null
  }

  private fun selectStatementAtCaretWithColumnNamesWildcard(
    file: SqlDelightFile,
    caret: Int
  ): SqliteSelectStmt? {
    val selectStatement = file.findElementOfTypeAtOffset<SqliteSelectStmt>(caret) ?: return null
    val resultColumns = selectStatement.resultColumnList
    return if (resultColumns.size == 1 && resultColumns[0].text == "*") selectStatement else null
  }

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    object : WriteCommandAction.Simple<Project>(project) {
      override fun run() {
        val caret = editor.caretModel.offset
        selectStatementAtCaretWithColumnNamesWildcard(file as SqlDelightFile, caret)?.run {
          val wildcard = resultColumnList.first()
          val allColumns = queryExposed()
              .flatMap { it.columns }
              .joinToString(separator = ", ") { it.element.text }
          editor.document.replaceString(wildcard.startOffset, wildcard.endOffset, allColumns)
        }
      }
    }.execute()
  }
}
