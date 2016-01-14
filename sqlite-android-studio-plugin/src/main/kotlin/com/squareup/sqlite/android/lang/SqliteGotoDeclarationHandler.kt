package com.squareup.sqlite.android.lang

import com.squareup.sqlite.android.SQLiteParser
import com.squareup.sqlite.android.SqliteCompiler
import com.squareup.sqlite.android.model.Column
import com.squareup.sqlite.android.model.SqlStmt
import com.squareup.sqlite.android.psi.SqliteElement.ColumnNameElement
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl

import com.squareup.sqlite.android.lang.SqliteTokenTypes.RULE_ELEMENT_TYPES
import com.squareup.sqlite.android.util.childOfType
import com.squareup.sqlite.android.util.childrenWithTokens
import com.squareup.sqlite.android.util.collectElements
import com.squareup.sqlite.android.util.elementType

class SqliteGotoDeclarationHandler : GotoDeclarationHandler {
  override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int,
      editor: Editor): Array<PsiElement>? {
    val resolveElement = (sourceElement?.parent as? PsiReferenceExpressionImpl)
        ?.advancedResolve(true)?.element as? PsiField ?: return emptyArray()
    val projectManager = ProjectRootManager.getInstance(resolveElement.project)
    val elementFile = resolveElement.containingFile.virtualFile

    // Only handle files under the generated sqlite directory.
    val sourceRoot = projectManager.fileIndex.getSourceRootForFile(elementFile)
    if (sourceRoot == null || !sourceRoot.path.endsWith(SqliteCompiler.OUTPUT_DIRECTORY)) {
      return emptyArray()
    }

    val identifier = resolveElement.childOfType<PsiIdentifier>() ?: return emptyArray()
    val psiManager = PsiManager.getInstance(resolveElement.project)
    var result: PsiElement? = null
    projectManager.fileIndex.iterateContent(SqliteContentIterator(psiManager) {
      if (SqliteCompiler.interfaceName(
          it.virtualFile.nameWithoutExtension) == elementFile.nameWithoutExtension) {
        result = it.collectElements({ it.isGeneratedFrom(identifier.text) }).firstOrNull()
      }
      result == null
    })

    return arrayOf(result ?: return emptyArray())
  }

  override fun getActionText(context: DataContext) = null

  private fun PsiElement.isGeneratedFrom(identifierText: String) =
      when {
        identifierText == SqliteCompiler.TABLE_NAME ->
          elementType === RULE_ELEMENT_TYPES[SQLiteParser.RULE_create_table_stmt]
        this is ColumnNameElement -> id?.name != null
            && Column.fieldName(id!!.name) == identifierText
            && getParent().elementType === RULE_ELEMENT_TYPES[SQLiteParser.RULE_column_def]
        elementType === RULE_ELEMENT_TYPES[SQLiteParser.RULE_sql_stmt] -> {
          val identifier = node.childrenWithTokens(SQLiteParser.IDENTIFIER)
          identifier.size > 0 && SqlStmt.fieldName(identifier[0].text) == identifierText
        }
        else -> false
      }
}
