package com.squareup.sqlite.android.psi

import com.intellij.psi.PsiElement
import com.squareup.sqlite.android.SQLiteParser
import com.squareup.sqlite.android.lang.SqliteFile
import com.squareup.sqlite.android.lang.SqliteTokenTypes.RULE_ELEMENT_TYPES
import com.squareup.sqlite.android.util.doRename
import com.squareup.sqlite.android.util.findUsages

internal class SqlStmtNameElementRef(idNode: IdentifierElement, ruleName: String)
: SqliteElementRef(idNode, ruleName) {
  override protected val identifierDefinitionRule = RULE_ELEMENT_TYPES[SQLiteParser.RULE_sql_stmt]

  override fun handleElementRename(newElementName: String): PsiElement {
    val file = myElement.containingFile as SqliteFile

    val usageInfo = myElement.findUsages(newElementName)
    myElement.doRename(newElementName, usageInfo, file, null)

    return myElement
  }
}
