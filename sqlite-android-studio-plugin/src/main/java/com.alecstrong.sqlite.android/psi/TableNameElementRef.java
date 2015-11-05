package com.alecstrong.sqlite.android.psi;

import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes;
import com.intellij.psi.tree.IElementType;

public class TableNameElementRef extends SqliteElementRef {
  public TableNameElementRef(IdentifierElement idNode, String ruleName) {
    super(idNode, ruleName);
  }

  @Override public Class<? extends SqliteElement> identifierParentClass() {
    return TableNameElement.class;
  }

  @Override public IElementType identifierDefinitionRule() {
    return SqliteTokenTypes.RULE_ELEMENT_TYPES.get(SQLiteParser.RULE_create_table_stmt);
  }
}
