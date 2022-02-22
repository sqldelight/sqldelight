package app.cash.sqldelight.core.lang.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference

abstract class JavaTypeMixin(
  node: ASTNode
) : ASTWrapperPsiElement(node) {
  override fun getReference(): PsiReference {
    return PsiMultiReference(references, this)
  }

  override fun getReferences(): Array<PsiReference> {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this)
  }
}
