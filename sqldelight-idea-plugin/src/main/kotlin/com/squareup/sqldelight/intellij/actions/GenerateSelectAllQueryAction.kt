package com.squareup.sqldelight.intellij.actions

import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import com.squareup.sqldelight.core.lang.util.findChildOfType
import com.squareup.sqldelight.core.psi.SqlDelightStmtList

internal class GenerateSelectAllQueryAction : BaseGenerateAction(SelectAllHandler()) {

  class SelectAllHandler : CodeInsightActionHandler {
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
      val caretOffset = editor.caretModel.offset
      val createTableStmt = file.findElementAt(caretOffset)?.parentOfType<SqlCreateTableStmt>() ?: return

      val tableName = createTableStmt.tableName.name
      val selectAllTemplate = TemplateManagerImpl.listApplicableTemplates(TemplateActionContext.create(file, null, caretOffset, caretOffset, false))
        .first { it.key == "sel" }

      val stmtList = file.findChildOfType<SqlDelightStmtList>() ?: return
      insertNewLineAndCleanup(editor, stmtList)
      TemplateManager.getInstance(project).startTemplate(
        editor, selectAllTemplate, false, mapOf("table" to tableName), null
      )
    }
  }
}
