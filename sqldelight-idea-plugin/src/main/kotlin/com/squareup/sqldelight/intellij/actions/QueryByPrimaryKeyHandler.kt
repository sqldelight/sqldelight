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

class QueryByPrimaryKeyHandler(private val templateName: String) : CodeInsightActionHandler {
  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    val caretOffset = editor.caretModel.offset
    val createTableStmt = file.findElementAt(caretOffset)?.parentOfType<SqlCreateTableStmt>() ?: return

    val tableName = createTableStmt.tableName.name
    val pk = createTableStmt.columnDefList.firstOrNull { columnDef ->
      columnDef.columnConstraintList.any { columnConstraint ->
        columnConstraint.textMatches("PRIMARY KEY")
      }
    }?.columnName

    val template = TemplateManagerImpl.listApplicableTemplates(TemplateActionContext.create(file, null, caretOffset, caretOffset, false))
      .first { it.key == templateName }

    val args = mutableMapOf("table" to tableName)
    if (pk != null) {
      args += "pkey" to pk.name
    }

    val stmtList = file.findChildOfType<SqlDelightStmtList>() ?: return
    insertNewLineAndCleanup(editor, stmtList)
    TemplateManager.getInstance(project).startTemplate(
      editor, template, false, args, null
    )
  }
}
