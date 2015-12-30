package com.alecstrong.sqlite.android.psi

import com.alecstrong.sqlite.android.SQLiteParser.RULE_sqlite_class_name
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes.RULE_ELEMENT_TYPES
import com.alecstrong.sqlite.android.util.elementType
import com.alecstrong.sqlite.android.util.findFirstParent
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType

class ClassNameElement(type: IElementType, text: CharSequence) : LeafPsiElement(type, text) {
  override fun getReference() =
      if (findFirstParent { element -> element is ASTWrapperPsiElement
          && element.elementType === RULE_ELEMENT_TYPES[RULE_sqlite_class_name] } != null) {
        ClassNameElementRef(this, text)
      } else null
}
