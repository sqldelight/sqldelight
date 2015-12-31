package com.alecstrong.sqlite.android.psi

import com.alecstrong.sqlite.android.SQLiteLexer
import com.alecstrong.sqlite.android.lang.SqliteContentIterator
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes
import com.alecstrong.sqlite.android.util.SqlitePsiUtils
import com.alecstrong.sqlite.android.util.elementType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiElementFilter
import com.intellij.psi.util.PsiTreeUtil

internal abstract class SqliteElementRef(idNode: IdentifierElement, private val ruleName: String)
: PsiReferenceBase<IdentifierElement>(idNode, TextRange(0, ruleName.length)), PsiElementFilter {
  protected val psiManager = PsiManager.getInstance(element.project)
  protected val projectRootManager = ProjectRootManager.getInstance(element.project)

  abstract protected val identifierDefinitionRule: IElementType

  override fun getVariants(): Array<Any> {
    val ruleSpecNodes = hashSetOf<String>()
    projectRootManager.fileIndex.iterateContent(SqliteContentIterator(psiManager, {
      ruleSpecNodes.addAll(PsiTreeUtil.collectElements(it, this).map { it.text })
      true
    }))

    return ruleSpecNodes.toArray()
  }

  override fun resolve(): PsiElement? {
    var result: PsiElement? = null
    projectRootManager.fileIndex.iterateContent(SqliteContentIterator(psiManager, {
      result = PsiTreeUtil.collectElements(it, this).firstOrNull { it.text == ruleName }
      result == null
    }))

    return result
  }

  override fun isAccepted(element: PsiElement) = element is SqliteElement &&
      element.parent.elementType === identifierDefinitionRule

  override fun handleElementRename(newElementName: String): PsiElement {
    myElement.replace(SqlitePsiUtils.createLeafFromText(element.project, myElement.context,
        newElementName, SqliteTokenTypes.TOKEN_ELEMENT_TYPES[SQLiteLexer.IDENTIFIER]))
    return myElement
  }
}
