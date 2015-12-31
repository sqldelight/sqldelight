package com.alecstrong.sqlite.android.psi

import com.alecstrong.sqlite.android.SQLiteLexer
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes
import com.alecstrong.sqlite.android.util.SqlitePsiUtils
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

sealed class SqliteElement(node: ASTNode) : ASTWrapperPsiElement(node), PsiNamedElement {
  protected var hardcodedName: String? = null

  abstract protected val ruleRefType: IElementType

  val id: IdentifierElement?
    get() = PsiTreeUtil.findChildOfType(this, IdentifierElement::class.java)

  override fun getTextOffset() = id?.textOffset ?: super.getTextOffset()
  override fun getName() = hardcodedName ?: id?.text ?: "unknown-name"
  override fun setName(name: String): PsiElement {
    id?.replace(SqlitePsiUtils.createLeafFromText(project, context, name, ruleRefType))
    hardcodedName = name
    return this
  }

  override fun subtreeChanged() {
    super.subtreeChanged()
    hardcodedName = null
  }

  internal class TableNameElement(node: ASTNode) : SqliteElement(node) {
    override val ruleRefType = SqliteTokenTypes.TOKEN_ELEMENT_TYPES[SQLiteLexer.IDENTIFIER]

    fun isSameTable(other: TableNameElement?) = id?.name?.equals(other?.id?.name) ?: false
  }

  internal class ColumnNameElement(node: ASTNode) : SqliteElement(node) {
    override val ruleRefType = SqliteTokenTypes.TOKEN_ELEMENT_TYPES[SQLiteLexer.IDENTIFIER]
  }
}
