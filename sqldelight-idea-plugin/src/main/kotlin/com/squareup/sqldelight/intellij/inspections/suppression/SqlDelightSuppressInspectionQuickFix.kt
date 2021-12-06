package com.squareup.sqldelight.intellij.inspections.suppression

import app.cash.sqldelight.core.lang.SqlDelightLanguage
import app.cash.sqldelight.core.lang.util.childOfType
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.daemon.impl.actions.SuppressByCommentFix
import com.intellij.codeInspection.SuppressionUtilCore.SUPPRESS_INSPECTIONS_TAG_NAME
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfTypes
import com.squareup.sqldelight.core.psi.SqlDelightImportStmt
import com.squareup.sqldelight.core.psi.SqlDelightStmtIdentifier

internal class SqlDelightSuppressInspectionQuickFix(toolId: String) :
  SuppressByCommentFix(toolId, PsiElement::class.java) {

  override fun createSuppression(project: Project, element: PsiElement, container: PsiElement) {
    val commenter = LanguageCommenters.INSTANCE.forLanguage(SqlDelightLanguage)
    val prefix = commenter?.lineCommentPrefix ?: return
    val text = "$prefix$SUPPRESS_INSPECTIONS_TAG_NAME $myID"

    val comment = createCommentFromText(container, text)
    val anchor = container.stmtIdentifier ?: container
    val parent = container.parent
    parent.addBefore(comment, anchor)
    val psiParserFacade = PsiParserFacade.SERVICE.getInstance(project)
    val newLine = psiParserFacade.createWhiteSpaceFromText("\n")
    parent.addBefore(newLine, anchor)
  }

  private fun createCommentFromText(element: PsiElement, text: String): PsiElement {
    val psiFileFactory = PsiFileFactory.getInstance(element.project)
    val file = psiFileFactory.createFileFromText("_Dummy_", SqlDelightLanguage, text)
    return file.childOfType(SqlTypes.COMMENT)!!
  }

  private val PsiElement.stmtIdentifier: PsiElement?
    get() = PsiTreeUtil.skipWhitespacesBackward(this) as? SqlDelightStmtIdentifier

  override fun replaceSuppressionComments(container: PsiElement): Boolean {
    val comment = container.comment
    if (comment == null || !comment.isSuppressionComment) {
      return false
    }

    if (!FileModificationService.getInstance().preparePsiElementsForWrite(comment)) {
      return false
    }
    WriteAction.run<RuntimeException> {
      val oldComment = comment.text
      comment.replace(createCommentFromText(comment, "$oldComment,$myID"))
    }
    return true
  }

  private val PsiElement.isSuppressionComment: Boolean
    get() = commentPattern.matcher(text).matches()

  override fun getContainer(context: PsiElement?): PsiElement? {
    return context?.parentOfTypes(
      classes = arrayOf(
        SqlStmt::class,
        SqlDelightImportStmt::class,
        SqlDelightStmtIdentifier::class
      ),
      withSelf = true
    )
  }
}
