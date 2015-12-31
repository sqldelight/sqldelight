package com.alecstrong.sqlite.android.lang

import com.alecstrong.sqlite.android.SQLiteParser
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler

class SqliteTokenSetQuoteHandler : SimpleTokenSetQuoteHandler(
    SqliteTokenTypes.TOKEN_ELEMENT_TYPES[SQLiteParser.QUOTE],
    SqliteTokenTypes.TOKEN_ELEMENT_TYPES[SQLiteParser.STRING_LITERAL])
