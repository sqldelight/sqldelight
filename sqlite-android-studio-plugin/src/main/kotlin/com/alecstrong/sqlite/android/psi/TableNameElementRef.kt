package com.alecstrong.sqlite.android.psi

import com.alecstrong.sqlite.android.SQLiteParser.RULE_create_table_stmt
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes.RULE_ELEMENT_TYPES
import com.alecstrong.sqlite.android.psi.SqliteElement.TableNameElement

internal class TableNameElementRef(idNode: IdentifierElement, ruleName: String)
: SqliteElementRef(idNode, ruleName) {
  override protected val identifierDefinitionRule = RULE_ELEMENT_TYPES[RULE_create_table_stmt]
}
