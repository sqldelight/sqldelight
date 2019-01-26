package com.squareup.sqldelight.core.lang.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.psi.SqlDelightImportStmt

abstract class JavaTypeMixin(
  node: ASTNode
) : ASTWrapperPsiElement(node) {
  override fun getReference(): PsiReference = JavaTypeReference()

  private inner class JavaTypeReference : PsiReferenceBase<JavaTypeMixin>(
      this, TextRange(0, textLength)
  ) {
    override fun getVariants(): Array<Any> = emptyArray()
    override fun resolve(): PsiElement? {
      val qualifiedType = if (parent is SqlDelightImportStmt) {
        text
      } else {
        val prefix = text.substringBefore('.')
        (containingFile as SqlDelightFile).sqlStmtList
            ?.findChildrenOfType<ImportStmtMixin>()
            ?.firstOrNull { it.javaType.text.endsWith(prefix) }
            ?.javaType?.text?.plus(text.removePrefix(prefix)) ?: typeForThisPackage(text)
      }

      return JavaPsiFacade.getInstance(project).findClass(qualifiedType,
          findModuleForPsiElement(element)!!.getModuleWithDependenciesAndLibrariesScope(false))
    }

    private fun typeForThisPackage(text: String) = when (ColumnDefMixin.kotlinType(text)) {
      null -> "${(containingFile as SqlDelightFile).packageName}.$text"
      else -> text
    }
  }
}
