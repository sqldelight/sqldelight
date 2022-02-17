package app.cash.sqldelight.core.lang.psi

import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.core.psi.SqlDelightImportStmt
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase

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

      val module = findModuleForPsiElement(element) ?: return null
      return JavaPsiFacade.getInstance(project).findClass(
        qualifiedType,
        module.getModuleWithDependenciesAndLibrariesScope(false)
      )
    }

    private fun typeForThisPackage(text: String) = "${(containingFile as SqlDelightFile).packageName}.$text"
  }
}
