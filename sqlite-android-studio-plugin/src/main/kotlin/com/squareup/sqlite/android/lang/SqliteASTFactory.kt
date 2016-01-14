package com.squareup.sqlite.android.lang

import com.squareup.sqlite.android.SQLiteLexer
import com.squareup.sqlite.android.SQLiteParser
import com.squareup.sqlite.android.lang.SqliteTokenTypes.RULE_ELEMENT_TYPES
import com.squareup.sqlite.android.lang.SqliteTokenTypes.TOKEN_ELEMENT_TYPES
import com.squareup.sqlite.android.psi.ASTWrapperPsiElement
import com.squareup.sqlite.android.psi.ClassNameElement
import com.squareup.sqlite.android.psi.IdentifierElement
import com.squareup.sqlite.android.psi.ParseElement
import com.squareup.sqlite.android.psi.SqliteElement.ColumnNameElement
import com.squareup.sqlite.android.psi.SqliteElement.TableNameElement
import com.intellij.lang.ASTFactory
import com.intellij.lang.ASTNode
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType

class SqliteASTFactory : ASTFactory() {
  override fun createComposite(type: IElementType) =
      when (type) {
        is IFileElementType -> FileElement(type, null)
        else -> CompositeElement(type)
      }

  override fun createLeaf(type: IElementType, text: CharSequence) =
      when (type) {
        TOKEN_ELEMENT_TYPES[SQLiteLexer.IDENTIFIER] -> IdentifierElement(type, text)
        TOKEN_ELEMENT_TYPES[SQLiteLexer.STRING_LITERAL] -> ClassNameElement(type, text)
        else -> LeafPsiElement(type, text)
      }
}

private val factories = mapOf(
    RULE_ELEMENT_TYPES[SQLiteParser.RULE_parse] to ::ParseElement,
    RULE_ELEMENT_TYPES[SQLiteParser.RULE_table_name] to ::TableNameElement,
    RULE_ELEMENT_TYPES[SQLiteParser.RULE_column_name] to ::ColumnNameElement
)

internal fun ASTNode.asPSINode() = factories[elementType]?.call(this) ?: ASTWrapperPsiElement(this)

