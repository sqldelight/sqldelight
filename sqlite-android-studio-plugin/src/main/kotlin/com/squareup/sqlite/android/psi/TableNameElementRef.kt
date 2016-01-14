package com.squareup.sqlite.android.psi

import com.squareup.sqlite.android.SQLiteParser.RULE_create_table_stmt
import com.squareup.sqlite.android.lang.SqliteTokenTypes.RULE_ELEMENT_TYPES

internal class TableNameElementRef(idNode: IdentifierElement, ruleName: String)
: SqliteElementRef(idNode, ruleName) {
  override protected val identifierDefinitionRule = RULE_ELEMENT_TYPES[RULE_create_table_stmt]
}
