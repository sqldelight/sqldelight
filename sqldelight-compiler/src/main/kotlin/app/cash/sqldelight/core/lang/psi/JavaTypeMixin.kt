package app.cash.sqldelight.core.lang.psi

import app.cash.sqldelight.core.SqldelightParser
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.SqlNamedElementImpl
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference

abstract class JavaTypeMixin(
  node: ASTNode,
) : SqlNamedElementImpl(node) {
  override val parseRule: (PsiBuilder, Int) -> Boolean = SqldelightParser::java_type_real

  override fun getReference(): PsiReference? {
    return PsiMultiReference(references.ifEmpty { return null }, this)
  }

  override fun getReferences(): Array<PsiReference> {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this)
  }

  override fun getText() = node.text

  override fun setName(name: String): PsiElement {
    // This is only renaming the type, so we also need to prepend and additional packages
    // ahead of the type.
    val imports = text.substringBeforeLast('.')
    return if (imports.isEmpty()) {
      super.setName(name)
    } else {
      super.setName("$imports.$name")
    }
  }

  override fun queryAvailable(child: PsiElement) = emptyList<QueryResult>()
}
