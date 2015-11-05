package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.SQLiteParser;
import com.intellij.psi.tree.IElementType;
import java.util.Arrays;
import java.util.List;
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.RuleElementType;
import org.antlr.intellij.adaptor.lexer.TokenElementType;
import org.intellij.lang.annotations.MagicConstant;

public class SqliteTokenTypes {
  public static IElementType BAD_TOKEN_TYPE =
      new IElementType("BAD_TOKEN", SqliteLanguage.INSTANCE);

  public static final List<TokenElementType> TOKEN_ELEMENT_TYPES =
      ElementTypeFactory.getTokenElementTypes(SqliteLanguage.INSTANCE,
          Arrays.asList(SQLiteParser.tokenNames));
  public static final List<RuleElementType> RULE_ELEMENT_TYPES =
      ElementTypeFactory.getRuleElementTypes(SqliteLanguage.INSTANCE,
          Arrays.asList(SQLiteParser.ruleNames));

  public static RuleElementType getRuleElementType(
      @MagicConstant(valuesFromClass = SQLiteParser.class) int ruleIndex) {
    return RULE_ELEMENT_TYPES.get(ruleIndex);
  }

  public static TokenElementType getTokenElementType(
      @MagicConstant(valuesFromClass = SQLiteParser.class) int ruleIndex) {
    return TOKEN_ELEMENT_TYPES.get(ruleIndex);
  }
}
