package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.SQLiteParser;
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler;

public class SqliteTokenSetQuoteHandler extends SimpleTokenSetQuoteHandler {
  public SqliteTokenSetQuoteHandler() {
    super(SqliteTokenTypes.TOKEN_ELEMENT_TYPES.get(SQLiteParser.QUOTE),
        SqliteTokenTypes.TOKEN_ELEMENT_TYPES.get(SQLiteParser.STRING_LITERAL));
  }
}
