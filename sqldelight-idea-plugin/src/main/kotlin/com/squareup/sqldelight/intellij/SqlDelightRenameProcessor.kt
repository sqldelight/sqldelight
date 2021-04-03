package com.squareup.sqldelight.intellij

import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlViewName
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.psi.StmtIdentifier
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.KtFile

class SqlDelightRenameProcessor : RenamePsiElementProcessor() {
  override fun canProcessElement(element: PsiElement): Boolean {
    if (element.module == null ||
      !SqlDelightFileIndex.getInstance(element.module!!).isConfigured
    ) {
      return false
    }
    return when (element) {
      is StmtIdentifier, is SqlTableName, is SqlViewName -> true
      else -> false
    }
  }

  override fun renameElement(
    element: PsiElement,
    newName: String,
    usages: Array<out UsageInfo>,
    listener: RefactoringElementListener?
  ) {
    val newTypeName = newName.capitalize()
    val currentTypeName = when (element) {
      is StmtIdentifierMixin -> element.identifier()!!.text
      is SqlTableName -> element.name
      is SqlViewName -> element.name
      else -> throw AssertionError()
    }.capitalize()
    element.generatedTypes(currentTypeName).forEach { type ->
      element.references(type)
        .filter { it.element.containingFile.virtualFile != type.containingFile }
        .forEach { reference ->
          val currentName = reference.element.text
          reference.handleElementRename(currentName.replace(currentTypeName, newTypeName))
        }
    }
    super.renameElement(element, newName, usages, listener)
  }

  override fun findReferences(
    element: PsiElement,
    searchScope: SearchScope,
    searchInCommentsAndStrings: Boolean
  ): Collection<PsiReference> {
    if (element !is StmtIdentifierMixin) return super.findReferences(element, searchScope, searchInCommentsAndStrings)
    return element.generatedMethods().flatMap { element.references(it) }
  }

  private fun PsiElement.references(element: PsiElement): Collection<PsiReference> {
    val processor = RenamePsiElementProcessor.forElement(element)
    return processor.findReferences(element, GlobalSearchScope.projectScope(element.project), false)
      .filter { it.element.containingFile.virtualFile != generatedFile() }
  }

  private fun PsiElement.generatedTypes(name: String): Array<PsiClass> {
    val path = (containingFile as SqlDelightFile).let { "${it.generatedDir}/$name.kt" }
    val module = module ?: return emptyArray()
    val vFile = SqlDelightFileIndex.getInstance(module).contentRoot
      .findFileByRelativePath(path) ?: return emptyArray()
    val file = PsiManager.getInstance(project).findFile(vFile) as? KtFile ?: return emptyArray()
    return file.classes
  }
}
