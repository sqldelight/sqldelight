package com.squareup.sqldelight.intellij.intentions

import com.alecstrong.sql.psi.core.psi.SqlSelectStmt
import com.alecstrong.sql.psi.core.psi.SqlTableAlias
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.squareup.sqldelight.core.lang.util.findChildrenOfType

internal class IntroduceTableAliasIntention : BaseElementAtCaretIntentionAction() {

  private val regex = "(?=\\p{Upper})|(_)".toRegex()

  override fun getFamilyName(): String {
    return INTENTIONS_FAMILY_NAME_REFACTORINGS
  }

  override fun getText(): String {
    return "Introduce table alias"
  }

  override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
    val tableName = element.parentOfType<SqlTableName>(true) ?: return false
    val columnAlias = PsiTreeUtil.getNextSiblingOfType(tableName, SqlTableAlias::class.java)
    if (columnAlias != null) {
      return false
    }
    val sqlSelectStmt = tableName.parentOfType<SqlSelectStmt>() ?: return false
    return sqlSelectStmt.resultColumnList.none { it.textMatches("*") }
  }

  override fun invoke(project: Project, editor: Editor, element: PsiElement) {
    val sqlTableName = element.parentOfType<SqlTableName>(true) ?: return
    val sqlSelectStmt = sqlTableName.parentOfType<SqlSelectStmt>() ?: return

    val tableName = sqlTableName.name
    val aliasVariants = listOf(
      tableName.first().toString(),
      tableName.split(regex)
        .filter(String::isNotBlank)
        .joinToString("") { it.first().toLowerCase().toString() }
    )
      .distinct()

    val document = editor.document
    val tableNameTextRange = sqlTableName.textRange
    val tableNameRangeMarker = document.createRangeMarker(tableNameTextRange)
    val tableReferenceMarkers = sqlSelectStmt.findChildrenOfType<SqlTableName>()
      .filter {
        it.textMatches(tableName) && !it.textRange.equals(tableNameTextRange)
      }
      .map { document.createRangeMarker(it.textRange) }

    val callback = { alias: String ->
      WriteCommandAction.runWriteCommandAction(project) {
        document.replaceString(
          tableNameRangeMarker.startOffset, tableNameRangeMarker.endOffset, "$tableName $alias"
        )
        tableReferenceMarkers.forEach { rangeMarker ->
          document.replaceString(rangeMarker.startOffset, rangeMarker.endOffset, alias)
        }
      }
    }
    if (aliasVariants.size == 1) {
      callback(aliasVariants.first())
    } else {
      JBPopupFactory.getInstance()
        .createPopupChooserBuilder(aliasVariants)
        .setMovable(true)
        .setResizable(true)
        .setItemChosenCallback(callback)
        .createPopup()
        .showInBestPositionFor(editor)
    }
  }
}
