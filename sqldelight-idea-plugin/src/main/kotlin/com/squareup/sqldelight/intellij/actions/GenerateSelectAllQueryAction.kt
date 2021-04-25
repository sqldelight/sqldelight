package com.squareup.sqldelight.intellij.actions

import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.squareup.sqldelight.core.lang.SqlDelightFile
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class GenerateSelectAllQueryAction : CodeInsightAction() {

  override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
    val offset = editor.caretModel.offset
    return file is SqlDelightFile && file.findElementAt(offset)?.parentOfType<SqlCreateTableStmt>() != null
  }

  override fun getHandler(): CodeInsightActionHandler = SelectAllHandler()

  class SelectAllHandler : CodeInsightActionHandler {
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
      val caretOffset = editor.caretModel.offset
      val createTableStmt = file.findElementAt(caretOffset)?.parentOfType<SqlCreateTableStmt>() ?: return

      val tableName = createTableStmt.tableName.name
      val selectAllTemplate = TemplateManagerImpl.listApplicableTemplates(file, caretOffset, false)
        .first { it.key == "sel" }

      val document = editor.document
      var lastChild = file.lastChild
      if (lastChild is PsiWhiteSpace) {
        lastChild = PsiTreeUtil.skipWhitespacesAndCommentsBackward(lastChild)
      }
      val s = "\n\n"
      document.insertString(lastChild.endOffset, s)
      val startOffset = lastChild.endOffset + s.length
      document.replaceString(startOffset, document.textLength, "")
      editor.caretModel.moveToOffset(startOffset)
      editor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
      editor.selectionModel.removeSelection()

      TemplateManager.getInstance(project).startTemplate(
        editor, selectAllTemplate, false, mapOf("table" to tableName), null
      )
    }
  }
}
