package com.squareup.sqlite.android.lang

import com.squareup.sqlite.android.SQLiteParser
import java.util.Arrays
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory

object SqliteTokenTypes {
  val TOKEN_ELEMENT_TYPES = ElementTypeFactory.getTokenElementTypes(SqliteLanguage.INSTANCE,
      Arrays.asList(*SQLiteParser.tokenNames))
  val RULE_ELEMENT_TYPES = ElementTypeFactory.getRuleElementTypes(SqliteLanguage.INSTANCE,
      Arrays.asList(*SQLiteParser.ruleNames))
}
