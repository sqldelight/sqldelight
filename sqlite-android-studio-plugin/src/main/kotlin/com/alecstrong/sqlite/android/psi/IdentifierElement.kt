package com.alecstrong.sqlite.android.psi

import com.alecstrong.sqlite.android.SQLiteParser
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes
import com.alecstrong.sqlite.android.psi.SqliteElement.ColumnNameElement
import com.alecstrong.sqlite.android.psi.SqliteElement.TableNameElement
import com.alecstrong.sqlite.android.util.SqlitePsiUtils
import com.alecstrong.sqlite.android.util.parentOfType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType

class IdentifierElement(type: IElementType, text: CharSequence) : LeafPsiElement(type,
    text), PsiNamedElement {
  private val ruleRefType = SqliteTokenTypes.TOKEN_ELEMENT_TYPES[SQLiteParser.IDENTIFIER]

  private var hardcodedName: String? = null

  override fun getName() = hardcodedName ?: text

  override fun setName(name: String): PsiElement {
    replace(SqlitePsiUtils.createLeafFromText(project, context, name, ruleRefType))
    hardcodedName = name
    return this
  }

  override fun getReference(): PsiReference? =
      when {
        parentOfType<TableNameElement>() != null -> TableNameElementRef(this, text)
        parentOfType<ColumnNameElement>() != null -> ColumnNameElementRef(this, text)
        else -> null
      }

  override fun toString() = "${javaClass.simpleName}(${elementType.toString()})"
}
