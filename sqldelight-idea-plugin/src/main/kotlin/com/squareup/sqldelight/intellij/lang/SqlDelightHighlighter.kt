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

import com.alecstrong.sql.psi.core.SqlLexerAdapter
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class SqlDelightHighlighter : SyntaxHighlighterBase() {
  override fun getHighlightingLexer() = SqlLexerAdapter()

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

        SqlTypes.ABORT.index to SQLITE_KEYWORD,
        SqlTypes.ACTION.index to SQLITE_KEYWORD,
        SqlTypes.ADD.index to SQLITE_KEYWORD,
        SqlTypes.AFTER.index to SQLITE_KEYWORD,
        SqlTypes.ALL.index to SQLITE_KEYWORD,
        SqlTypes.ALTER.index to SQLITE_KEYWORD,
        SqlTypes.ANALYZE.index to SQLITE_KEYWORD,
        SqlTypes.AND.index to SQLITE_KEYWORD,
        SqlTypes.AS.index to SQLITE_KEYWORD,
        SqlTypes.ASC.index to SQLITE_KEYWORD,
        SqlTypes.ATTACH.index to SQLITE_KEYWORD,
        SqlTypes.AUTOINCREMENT.index to SQLITE_KEYWORD,
        SqlTypes.BEFORE.index to SQLITE_KEYWORD,
        SqlTypes.BEGIN.index to SQLITE_KEYWORD,
        SqlTypes.BETWEEN.index to SQLITE_KEYWORD,
        SqlTypes.BY.index to SQLITE_KEYWORD,
        SqlTypes.CASCADE.index to SQLITE_KEYWORD,
        SqlTypes.CASE.index to SQLITE_KEYWORD,
        SqlTypes.CAST.index to SQLITE_KEYWORD,
        SqlTypes.CHECK.index to SQLITE_KEYWORD,
        SqlTypes.COLLATE.index to SQLITE_KEYWORD,
        SqlTypes.COLUMN.index to SQLITE_KEYWORD,
        SqlTypes.COMMIT.index to SQLITE_KEYWORD,
        SqlTypes.CONFLICT.index to SQLITE_KEYWORD,
        SqlTypes.CONSTRAINT.index to SQLITE_KEYWORD,
        SqlTypes.CREATE.index to SQLITE_KEYWORD,
        SqlTypes.CROSS.index to SQLITE_KEYWORD,
        SqlTypes.CURRENT_DATE.index to SQLITE_KEYWORD,
        SqlTypes.CURRENT_TIME.index to SQLITE_KEYWORD,
        SqlTypes.CURRENT_TIMESTAMP.index to SQLITE_KEYWORD,
        SqlTypes.DATABASE.index to SQLITE_KEYWORD,
        SqlTypes.DEFAULT.index to SQLITE_KEYWORD,
        SqlTypes.DEFERRABLE.index to SQLITE_KEYWORD,
        SqlTypes.DEFERRED.index to SQLITE_KEYWORD,
        SqlTypes.DELETE.index to SQLITE_KEYWORD,
        SqlTypes.DESC.index to SQLITE_KEYWORD,
        SqlTypes.DETACH.index to SQLITE_KEYWORD,
        SqlTypes.DISTINCT.index to SQLITE_KEYWORD,
        SqlTypes.DROP.index to SQLITE_KEYWORD,
        SqlTypes.EACH.index to SQLITE_KEYWORD,
        SqlTypes.ELSE.index to SQLITE_KEYWORD,
        SqlTypes.END.index to SQLITE_KEYWORD,
        SqlTypes.ESCAPE.index to SQLITE_KEYWORD,
        SqlTypes.EXCEPT.index to SQLITE_KEYWORD,
        SqlTypes.EXCLUSIVE.index to SQLITE_KEYWORD,
        SqlTypes.EXISTS.index to SQLITE_KEYWORD,
        SqlTypes.EXPLAIN.index to SQLITE_KEYWORD,
        SqlTypes.FAIL.index to SQLITE_KEYWORD,
        SqlTypes.FOR.index to SQLITE_KEYWORD,
        SqlTypes.FOREIGN.index to SQLITE_KEYWORD,
        SqlTypes.FROM.index to SQLITE_KEYWORD,
        SqlTypes.GLOB.index to SQLITE_KEYWORD,
        SqlTypes.GROUP.index to SQLITE_KEYWORD,
        SqlTypes.HAVING.index to SQLITE_KEYWORD,
        SqlTypes.IF.index to SQLITE_KEYWORD,
        SqlTypes.IGNORE.index to SQLITE_KEYWORD,
        SqlTypes.IMMEDIATE.index to SQLITE_KEYWORD,
        SqlTypes.IN.index to SQLITE_KEYWORD,
        SqlTypes.INDEX.index to SQLITE_KEYWORD,
        SqlTypes.INDEXED.index to SQLITE_KEYWORD,
        SqlTypes.INITIALLY.index to SQLITE_KEYWORD,
        SqlTypes.INNER.index to SQLITE_KEYWORD,
        SqlTypes.INSERT.index to SQLITE_KEYWORD,
        SqlTypes.INSTEAD.index to SQLITE_KEYWORD,
        SqlTypes.INTERSECT.index to SQLITE_KEYWORD,
        SqlTypes.INTO.index to SQLITE_KEYWORD,
        SqlTypes.IS.index to SQLITE_KEYWORD,
        SqlTypes.ISNULL.index to SQLITE_KEYWORD,
        SqlTypes.JOIN.index to SQLITE_KEYWORD,
        SqlTypes.KEY.index to SQLITE_KEYWORD,
        SqlTypes.LEFT.index to SQLITE_KEYWORD,
        SqlTypes.LIKE.index to SQLITE_KEYWORD,
        SqlTypes.LIMIT.index to SQLITE_KEYWORD,
        SqlTypes.MATCH.index to SQLITE_KEYWORD,
        SqlTypes.NATURAL.index to SQLITE_KEYWORD,
        SqlTypes.NO.index to SQLITE_KEYWORD,
        SqlTypes.NOT.index to SQLITE_KEYWORD,
        SqlTypes.NOTNULL.index to SQLITE_KEYWORD,
        SqlTypes.NULL.index to SQLITE_KEYWORD,
        SqlTypes.OF.index to SQLITE_KEYWORD,
        SqlTypes.OFFSET.index to SQLITE_KEYWORD,
        SqlTypes.ON.index to SQLITE_KEYWORD,
        SqlTypes.OR.index to SQLITE_KEYWORD,
        SqlTypes.ORDER.index to SQLITE_KEYWORD,
        SqlTypes.OUTER.index to SQLITE_KEYWORD,
        SqlTypes.PLAN.index to SQLITE_KEYWORD,
        SqlTypes.PRAGMA.index to SQLITE_KEYWORD,
        SqlTypes.PRIMARY.index to SQLITE_KEYWORD,
        SqlTypes.QUERY.index to SQLITE_KEYWORD,
        SqlTypes.RAISE.index to SQLITE_KEYWORD,
        SqlTypes.RECURSIVE.index to SQLITE_KEYWORD,
        SqlTypes.REFERENCES.index to SQLITE_KEYWORD,
        SqlTypes.REGEXP.index to SQLITE_KEYWORD,
        SqlTypes.REINDEX.index to SQLITE_KEYWORD,
        SqlTypes.RELEASE.index to SQLITE_KEYWORD,
        SqlTypes.RENAME.index to SQLITE_KEYWORD,
        SqlTypes.REPLACE.index to SQLITE_KEYWORD,
        SqlTypes.RESTRICT.index to SQLITE_KEYWORD,
        SqlTypes.ROLLBACK.index to SQLITE_KEYWORD,
        SqlTypes.ROW.index to SQLITE_KEYWORD,
        SqlTypes.SAVEPOINT.index to SQLITE_KEYWORD,
        SqlTypes.SELECT.index to SQLITE_KEYWORD,
        SqlTypes.SET.index to SQLITE_KEYWORD,
        SqlTypes.TABLE.index to SQLITE_KEYWORD,
        SqlTypes.TEMP.index to SQLITE_KEYWORD,
        SqlTypes.TEMPORARY.index to SQLITE_KEYWORD,
        SqlTypes.THEN.index to SQLITE_KEYWORD,
        SqlTypes.TO.index to SQLITE_KEYWORD,
        SqlTypes.TRANSACTION.index to SQLITE_KEYWORD,
        SqlTypes.TRIGGER.index to SQLITE_KEYWORD,
        SqlTypes.UNION.index to SQLITE_KEYWORD,
        SqlTypes.UNIQUE.index to SQLITE_KEYWORD,
        SqlTypes.UPDATE.index to SQLITE_KEYWORD,
        SqlTypes.USING.index to SQLITE_KEYWORD,
        SqlTypes.VACUUM.index to SQLITE_KEYWORD,
        SqlTypes.VALUES.index to SQLITE_KEYWORD,
        SqlTypes.VIEW.index to SQLITE_KEYWORD,
        SqlTypes.VIRTUAL.index to SQLITE_KEYWORD,
        SqlTypes.WHEN.index to SQLITE_KEYWORD,
        SqlTypes.WHERE.index to SQLITE_KEYWORD,
        SqlTypes.WITH.index to SQLITE_KEYWORD,
        SqlTypes.WITHOUT.index to SQLITE_KEYWORD,

        // Numbers

        SqlTypes.NUMERIC_LITERAL.index to SQLITE_NUMBER,
        SqlTypes.SIGNED_NUMBER.index to SQLITE_NUMBER,
        SqlTypes.DIGIT.index to SQLITE_NUMBER,

        // Operators

        SqlTypes.COMMA.index to SQLITE_COMMA,
        SqlTypes.DOT.index to SQLITE_DOT,
        SqlTypes.EQ.index to SQLITE_OPERATOR,
        SqlTypes.LP.index to SQLITE_PAREN,
        SqlTypes.RP.index to SQLITE_PAREN,
        SqlTypes.SEMI.index to SQLITE_SEMICOLON,
        SqlTypes.PLUS.index to SQLITE_OPERATOR,
        SqlTypes.MINUS.index to SQLITE_OPERATOR,
        SqlTypes.SHIFT_RIGHT.index to SQLITE_OPERATOR,
        SqlTypes.SHIFT_LEFT.index to SQLITE_OPERATOR,
        SqlTypes.LT.index to SQLITE_OPERATOR,
        SqlTypes.GT.index to SQLITE_OPERATOR,
        SqlTypes.LTE.index to SQLITE_OPERATOR,
        SqlTypes.GTE.index to SQLITE_OPERATOR,
        SqlTypes.EQ2.index to SQLITE_OPERATOR,
        SqlTypes.NEQ.index to SQLITE_OPERATOR,
        SqlTypes.NEQ2.index to SQLITE_OPERATOR,
        SqlTypes.MULTIPLY.index to SQLITE_OPERATOR,
        SqlTypes.DIVIDE.index to SQLITE_OPERATOR,
        SqlTypes.MOD.index to SQLITE_OPERATOR,
        SqlTypes.BITWISE_AND.index to SQLITE_OPERATOR,
        SqlTypes.BITWISE_OR.index to SQLITE_OPERATOR,
        SqlTypes.CONCAT.index to SQLITE_OPERATOR,

        SqlTypes.STRING.index to SQLITE_STRING,

        // Comments

        SqlTypes.COMMENT.index to SQLITE_LINE_COMMENT,
        SqlTypes.JAVADOC.index to SQLITE_DOC
    )
  }
}
