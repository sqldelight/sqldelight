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
package com.squareup.sqldelight.lang

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.squareup.sqldelight.SqliteLexer
import org.antlr.intellij.adaptor.lexer.SimpleAntlrAdapter
import org.antlr.intellij.adaptor.lexer.TokenElementType

class SqliteHighlighter : SyntaxHighlighterBase() {
  override fun getHighlightingLexer() =
      SimpleAntlrAdapter(SqliteLanguage.INSTANCE, SqliteLexer(null))

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

    private val LAST_TOKEN = SqliteLexer.UNEXPECTED_CHAR
    private val textAttributesKey = arrayOfNulls<TextAttributesKey>(LAST_TOKEN + 1)

    init {
      for (i in SqliteLexer.K_ABORT..SqliteLexer.K_WITHOUT) {
        textAttributesKey[i] = SQLITE_KEYWORD
      }
      for (i in SqliteLexer.ASSIGN..SqliteLexer.NOT_EQ2) {
        textAttributesKey[i] = SQLITE_OPERATOR
      }
      textAttributesKey[SqliteLexer.NUMERIC_LITERAL] = SQLITE_NUMBER
      textAttributesKey[SqliteLexer.IDENTIFIER] = SQLITE_IDENTIFIER
      textAttributesKey[SqliteLexer.STRING_LITERAL] = SQLITE_STRING
      textAttributesKey[SqliteLexer.SINGLE_LINE_COMMENT] = SQLITE_LINE_COMMENT
      textAttributesKey[SqliteLexer.MULTILINE_COMMENT] = SQLITE_MULTILINE_COMMENT
      textAttributesKey[SqliteLexer.OPEN_PAR] = SQLITE_PAREN
      textAttributesKey[SqliteLexer.CLOSE_PAR] = SQLITE_PAREN
      textAttributesKey[SqliteLexer.DOT] = SQLITE_DOT
      textAttributesKey[SqliteLexer.SCOL] = SQLITE_SEMICOLON
      textAttributesKey[SqliteLexer.COMMA] = SQLITE_COMMA
      textAttributesKey[SqliteLexer.UNEXPECTED_CHAR] = HighlighterColors.BAD_CHARACTER
    }
  }
}
