/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqlite.android.lang

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.squareup.sqlite.android.SQLiteLexer
import org.antlr.intellij.adaptor.lexer.SimpleAntlrAdapter
import org.antlr.intellij.adaptor.lexer.TokenElementType

class SqliteHighlighter : SyntaxHighlighterBase() {
  override fun getHighlightingLexer() =
      SimpleAntlrAdapter(SqliteLanguage.INSTANCE, SQLiteLexer(null))

  override fun getTokenHighlights(tokenType: IElementType) =
      when (tokenType) {
        is TokenElementType -> SyntaxHighlighterBase.pack(textAttributesKey[tokenType.type])
        else -> emptyArray<TextAttributesKey>()
      }

  companion object {
    private val SQLITE_KEYWORD = TextAttributesKey.createTextAttributesKey(
        "SQLITE.KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
    private val SQLITE_NUMBER = TextAttributesKey.createTextAttributesKey(
        "SQLITE.NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    private val SQLITE_IDENTIFIER = TextAttributesKey.createTextAttributesKey(
        "SQLITE.IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
    private val SQLITE_STRING = TextAttributesKey.createTextAttributesKey(
        "SQLITE.STRING", DefaultLanguageHighlighterColors.STRING)
    private val SQLITE_LINE_COMMENT = TextAttributesKey.createTextAttributesKey(
        "SQLITE.LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    private val SQLITE_MULTILINE_COMMENT = TextAttributesKey.createTextAttributesKey(
        "SQLITE.MULTILINE_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
    private val SQLITE_OPERATOR = TextAttributesKey.createTextAttributesKey(
        "SQLITE.OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    private val SQLITE_PAREN = TextAttributesKey.createTextAttributesKey(
        "SQLITE.PAREN", DefaultLanguageHighlighterColors.PARENTHESES)
    private val SQLITE_DOT = TextAttributesKey.createTextAttributesKey(
        "SQLITE.DOT", DefaultLanguageHighlighterColors.DOT)
    private val SQLITE_SEMICOLON = TextAttributesKey.createTextAttributesKey(
        "SQLITE.SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
    private val SQLITE_COMMA = TextAttributesKey.createTextAttributesKey(
        "SQLITE.COMMA", DefaultLanguageHighlighterColors.COMMA)

    private val LAST_TOKEN = SQLiteLexer.UNEXPECTED_CHAR
    private val textAttributesKey = arrayOfNulls<TextAttributesKey>(LAST_TOKEN + 1)

    init {
      for (i in SQLiteLexer.K_ABORT..SQLiteLexer.K_WITHOUT) {
        textAttributesKey[i] = SQLITE_KEYWORD
      }
      for (i in SQLiteLexer.ASSIGN..SQLiteLexer.NOT_EQ2) {
        textAttributesKey[i] = SQLITE_OPERATOR
      }
      textAttributesKey[SQLiteLexer.NUMERIC_LITERAL] = SQLITE_NUMBER
      textAttributesKey[SQLiteLexer.IDENTIFIER] = SQLITE_IDENTIFIER
      textAttributesKey[SQLiteLexer.STRING_LITERAL] = SQLITE_STRING
      textAttributesKey[SQLiteLexer.SINGLE_LINE_COMMENT] = SQLITE_LINE_COMMENT
      textAttributesKey[SQLiteLexer.MULTILINE_COMMENT] = SQLITE_MULTILINE_COMMENT
      textAttributesKey[SQLiteLexer.OPEN_PAR] = SQLITE_PAREN
      textAttributesKey[SQLiteLexer.CLOSE_PAR] = SQLITE_PAREN
      textAttributesKey[SQLiteLexer.DOT] = SQLITE_DOT
      textAttributesKey[SQLiteLexer.SCOL] = SQLITE_SEMICOLON
      textAttributesKey[SQLiteLexer.COMMA] = SQLITE_COMMA
      textAttributesKey[SQLiteLexer.UNEXPECTED_CHAR] = HighlighterColors.BAD_CHARACTER
    }
  }
}
