package com.squareup.sqldelight.intellij.actions

import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import com.squareup.sqldelight.core.lang.SqlDelightFile

internal abstract class BaseGenerateAction(private val handler: CodeInsightActionHandler) : CodeInsightAction() {

  override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
    val offset = editor.caretModel.offset
    return file is SqlDelightFile && file.findElementAt(offset)
      ?.parentOfType<SqlCreateTableStmt>(true) != null
  }

  override fun getHandler(): CodeInsightActionHandler = handler
}
