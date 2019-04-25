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
package com.squareup.sqldelight.intellij.lang

import com.alecstrong.sqlite.psi.core.SqliteLexerAdapter
import com.alecstrong.sqlite.psi.core.psi.SqliteTypes
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class SqlDelightHighlighter : SyntaxHighlighterBase() {
  override fun getHighlightingLexer() = SqliteLexerAdapter()

  override fun getTokenHighlights(tokenType: IElementType) =
    SyntaxHighlighterBase.pack(TEXT_ATTRIBUTES_MAP[tokenType.index])

  companion object {
    private val SQLITE_KEYWORD = TextAttributesKey.createTextAttributesKey(
        "SQLITE.KEYWORD", DefaultLanguageHighlighterColors.KEYWORD
    )
    private val SQLITE_NUMBER = TextAttributesKey.createTextAttributesKey(
        "SQLITE.NUMBER", DefaultLanguageHighlighterColors.NUMBER
    )
    private val SQLITE_STRING = TextAttributesKey.createTextAttributesKey(
        "SQLITE.STRING", DefaultLanguageHighlighterColors.STRING
    )
    private val SQLITE_OPERATOR = TextAttributesKey.createTextAttributesKey(
        "SQLITE.OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN
    )
    private val SQLITE_PAREN = TextAttributesKey.createTextAttributesKey(
        "SQLITE.PAREN", DefaultLanguageHighlighterColors.PARENTHESES
    )
    private val SQLITE_DOT = TextAttributesKey.createTextAttributesKey(
        "SQLITE.DOT", DefaultLanguageHighlighterColors.DOT
    )
    private val SQLITE_SEMICOLON = TextAttributesKey.createTextAttributesKey(
        "SQLITE.SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON
    )
    private val SQLITE_COMMA = TextAttributesKey.createTextAttributesKey(
        "SQLITE.COMMA", DefaultLanguageHighlighterColors.COMMA
    )
    private val SQLITE_LINE_COMMENT = TextAttributesKey.createTextAttributesKey(
        "SQLITE.LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT
    )
    private val SQLITE_DOC = TextAttributesKey.createTextAttributesKey(
        "SQLITE.DOC", DefaultLanguageHighlighterColors.DOC_COMMENT
    )

    private val TEXT_ATTRIBUTES_MAP = mapOf(

        // Keywords

        SqliteTypes.ABORT.index to SQLITE_KEYWORD,
        SqliteTypes.ACTION.index to SQLITE_KEYWORD,
        SqliteTypes.ADD.index to SQLITE_KEYWORD,
        SqliteTypes.AFTER.index to SQLITE_KEYWORD,
        SqliteTypes.ALL.index to SQLITE_KEYWORD,
        SqliteTypes.ALTER.index to SQLITE_KEYWORD,
        SqliteTypes.ANALYZE.index to SQLITE_KEYWORD,
        SqliteTypes.AND.index to SQLITE_KEYWORD,
        SqliteTypes.AS.index to SQLITE_KEYWORD,
        SqliteTypes.ASC.index to SQLITE_KEYWORD,
        SqliteTypes.ATTACH.index to SQLITE_KEYWORD,
        SqliteTypes.AUTOINCREMENT.index to SQLITE_KEYWORD,
        SqliteTypes.BEFORE.index to SQLITE_KEYWORD,
        SqliteTypes.BEGIN.index to SQLITE_KEYWORD,
        SqliteTypes.BETWEEN.index to SQLITE_KEYWORD,
        SqliteTypes.BY.index to SQLITE_KEYWORD,
        SqliteTypes.CASCADE.index to SQLITE_KEYWORD,
        SqliteTypes.CASE.index to SQLITE_KEYWORD,
        SqliteTypes.CAST.index to SQLITE_KEYWORD,
        SqliteTypes.CHECK.index to SQLITE_KEYWORD,
        SqliteTypes.COLLATE.index to SQLITE_KEYWORD,
        SqliteTypes.COLUMN.index to SQLITE_KEYWORD,
        SqliteTypes.COMMIT.index to SQLITE_KEYWORD,
        SqliteTypes.CONFLICT.index to SQLITE_KEYWORD,
        SqliteTypes.CONSTRAINT.index to SQLITE_KEYWORD,
        SqliteTypes.CREATE.index to SQLITE_KEYWORD,
        SqliteTypes.CROSS.index to SQLITE_KEYWORD,
        SqliteTypes.CURRENT_DATE.index to SQLITE_KEYWORD,
        SqliteTypes.CURRENT_TIME.index to SQLITE_KEYWORD,
        SqliteTypes.CURRENT_TIMESTAMP.index to SQLITE_KEYWORD,
        SqliteTypes.DATABASE.index to SQLITE_KEYWORD,
        SqliteTypes.DEFAULT.index to SQLITE_KEYWORD,
        SqliteTypes.DEFERRABLE.index to SQLITE_KEYWORD,
        SqliteTypes.DEFERRED.index to SQLITE_KEYWORD,
        SqliteTypes.DELETE.index to SQLITE_KEYWORD,
        SqliteTypes.DESC.index to SQLITE_KEYWORD,
        SqliteTypes.DETACH.index to SQLITE_KEYWORD,
        SqliteTypes.DISTINCT.index to SQLITE_KEYWORD,
        SqliteTypes.DROP.index to SQLITE_KEYWORD,
        SqliteTypes.EACH.index to SQLITE_KEYWORD,
        SqliteTypes.ELSE.index to SQLITE_KEYWORD,
        SqliteTypes.END.index to SQLITE_KEYWORD,
        SqliteTypes.ESCAPE.index to SQLITE_KEYWORD,
        SqliteTypes.EXCEPT.index to SQLITE_KEYWORD,
        SqliteTypes.EXCLUSIVE.index to SQLITE_KEYWORD,
        SqliteTypes.EXISTS.index to SQLITE_KEYWORD,
        SqliteTypes.EXPLAIN.index to SQLITE_KEYWORD,
        SqliteTypes.FAIL.index to SQLITE_KEYWORD,
        SqliteTypes.FOR.index to SQLITE_KEYWORD,
        SqliteTypes.FOREIGN.index to SQLITE_KEYWORD,
        SqliteTypes.FROM.index to SQLITE_KEYWORD,
        SqliteTypes.GLOB.index to SQLITE_KEYWORD,
        SqliteTypes.GROUP.index to SQLITE_KEYWORD,
        SqliteTypes.HAVING.index to SQLITE_KEYWORD,
        SqliteTypes.IF.index to SQLITE_KEYWORD,
        SqliteTypes.IGNORE.index to SQLITE_KEYWORD,
        SqliteTypes.IMMEDIATE.index to SQLITE_KEYWORD,
        SqliteTypes.IN.index to SQLITE_KEYWORD,
        SqliteTypes.INDEX.index to SQLITE_KEYWORD,
        SqliteTypes.INDEXED.index to SQLITE_KEYWORD,
        SqliteTypes.INITIALLY.index to SQLITE_KEYWORD,
        SqliteTypes.INNER.index to SQLITE_KEYWORD,
        SqliteTypes.INSERT.index to SQLITE_KEYWORD,
        SqliteTypes.INSTEAD.index to SQLITE_KEYWORD,
        SqliteTypes.INTERSECT.index to SQLITE_KEYWORD,
        SqliteTypes.INTO.index to SQLITE_KEYWORD,
        SqliteTypes.IS.index to SQLITE_KEYWORD,
        SqliteTypes.ISNULL.index to SQLITE_KEYWORD,
        SqliteTypes.JOIN.index to SQLITE_KEYWORD,
        SqliteTypes.KEY.index to SQLITE_KEYWORD,
        SqliteTypes.LEFT.index to SQLITE_KEYWORD,
        SqliteTypes.LIKE.index to SQLITE_KEYWORD,
        SqliteTypes.LIMIT.index to SQLITE_KEYWORD,
        SqliteTypes.MATCH.index to SQLITE_KEYWORD,
        SqliteTypes.NATURAL.index to SQLITE_KEYWORD,
        SqliteTypes.NO.index to SQLITE_KEYWORD,
        SqliteTypes.NOT.index to SQLITE_KEYWORD,
        SqliteTypes.NOTNULL.index to SQLITE_KEYWORD,
        SqliteTypes.NULL.index to SQLITE_KEYWORD,
        SqliteTypes.OF.index to SQLITE_KEYWORD,
        SqliteTypes.OFFSET.index to SQLITE_KEYWORD,
        SqliteTypes.ON.index to SQLITE_KEYWORD,
        SqliteTypes.OR.index to SQLITE_KEYWORD,
        SqliteTypes.ORDER.index to SQLITE_KEYWORD,
        SqliteTypes.OUTER.index to SQLITE_KEYWORD,
        SqliteTypes.PLAN.index to SQLITE_KEYWORD,
        SqliteTypes.PRAGMA.index to SQLITE_KEYWORD,
        SqliteTypes.PRIMARY.index to SQLITE_KEYWORD,
        SqliteTypes.QUERY.index to SQLITE_KEYWORD,
        SqliteTypes.RAISE.index to SQLITE_KEYWORD,
        SqliteTypes.RECURSIVE.index to SQLITE_KEYWORD,
        SqliteTypes.REFERENCES.index to SQLITE_KEYWORD,
        SqliteTypes.REGEXP.index to SQLITE_KEYWORD,
        SqliteTypes.REINDEX.index to SQLITE_KEYWORD,
        SqliteTypes.RELEASE.index to SQLITE_KEYWORD,
        SqliteTypes.RENAME.index to SQLITE_KEYWORD,
        SqliteTypes.REPLACE.index to SQLITE_KEYWORD,
        SqliteTypes.RESTRICT.index to SQLITE_KEYWORD,
        SqliteTypes.ROLLBACK.index to SQLITE_KEYWORD,
        SqliteTypes.ROW.index to SQLITE_KEYWORD,
        SqliteTypes.SAVEPOINT.index to SQLITE_KEYWORD,
        SqliteTypes.SELECT.index to SQLITE_KEYWORD,
        SqliteTypes.SET.index to SQLITE_KEYWORD,
        SqliteTypes.TABLE.index to SQLITE_KEYWORD,
        SqliteTypes.TEMP.index to SQLITE_KEYWORD,
        SqliteTypes.TEMPORARY.index to SQLITE_KEYWORD,
        SqliteTypes.THEN.index to SQLITE_KEYWORD,
        SqliteTypes.TO.index to SQLITE_KEYWORD,
        SqliteTypes.TRANSACTION.index to SQLITE_KEYWORD,
        SqliteTypes.TRIGGER.index to SQLITE_KEYWORD,
        SqliteTypes.UNION.index to SQLITE_KEYWORD,
        SqliteTypes.UNIQUE.index to SQLITE_KEYWORD,
        SqliteTypes.UPDATE.index to SQLITE_KEYWORD,
        SqliteTypes.USING.index to SQLITE_KEYWORD,
        SqliteTypes.VACUUM.index to SQLITE_KEYWORD,
        SqliteTypes.VALUES.index to SQLITE_KEYWORD,
        SqliteTypes.VIEW.index to SQLITE_KEYWORD,
        SqliteTypes.VIRTUAL.index to SQLITE_KEYWORD,
        SqliteTypes.WHEN.index to SQLITE_KEYWORD,
        SqliteTypes.WHERE.index to SQLITE_KEYWORD,
        SqliteTypes.WITH.index to SQLITE_KEYWORD,
        SqliteTypes.WITHOUT.index to SQLITE_KEYWORD,

        // Numbers

        SqliteTypes.NUMERIC_LITERAL.index to SQLITE_NUMBER,
        SqliteTypes.SIGNED_NUMBER.index to SQLITE_NUMBER,
        SqliteTypes.DIGIT.index to SQLITE_NUMBER,

        // Operators

        SqliteTypes.COMMA.index to SQLITE_COMMA,
        SqliteTypes.DOT.index to SQLITE_DOT,
        SqliteTypes.EQ.index to SQLITE_OPERATOR,
        SqliteTypes.LP.index to SQLITE_PAREN,
        SqliteTypes.RP.index to SQLITE_PAREN,
        SqliteTypes.SEMI.index to SQLITE_SEMICOLON,
        SqliteTypes.PLUS.index to SQLITE_OPERATOR,
        SqliteTypes.MINUS.index to SQLITE_OPERATOR,
        SqliteTypes.SHIFT_RIGHT.index to SQLITE_OPERATOR,
        SqliteTypes.SHIFT_LEFT.index to SQLITE_OPERATOR,
        SqliteTypes.LT.index to SQLITE_OPERATOR,
        SqliteTypes.GT.index to SQLITE_OPERATOR,
        SqliteTypes.LTE.index to SQLITE_OPERATOR,
        SqliteTypes.GTE.index to SQLITE_OPERATOR,
        SqliteTypes.EQ2.index to SQLITE_OPERATOR,
        SqliteTypes.NEQ.index to SQLITE_OPERATOR,
        SqliteTypes.NEQ2.index to SQLITE_OPERATOR,
        SqliteTypes.MULTIPLY.index to SQLITE_OPERATOR,
        SqliteTypes.DIVIDE.index to SQLITE_OPERATOR,
        SqliteTypes.MOD.index to SQLITE_OPERATOR,
        SqliteTypes.BITWISE_AND.index to SQLITE_OPERATOR,
        SqliteTypes.BITWISE_OR.index to SQLITE_OPERATOR,
        SqliteTypes.CONCAT.index to SQLITE_OPERATOR,

        SqliteTypes.STRING.index to SQLITE_STRING,

        // Comments

        SqliteTypes.COMMENT.index to SQLITE_LINE_COMMENT,
        SqliteTypes.JAVADOC.index to SQLITE_DOC
    )
  }
}
