package app.cash.sqldelight.intellij

import app.cash.sqldelight.intellij.lang.SqlDelightHighlightVisitor.Companion.SQL_ALIAS
import app.cash.sqldelight.intellij.lang.SqlDelightHighlightVisitor.Companion.SQL_COLUMN_NAME
import app.cash.sqldelight.intellij.lang.SqlDelightHighlightVisitor.Companion.SQL_FUNCTION_NAME
import app.cash.sqldelight.intellij.lang.SqlDelightHighlightVisitor.Companion.SQL_STMT_IDENTIFIER_NAME
import app.cash.sqldelight.intellij.lang.SqlDelightHighlightVisitor.Companion.SQL_TABLE_NAME
import app.cash.sqldelight.intellij.lang.SqlDelightHighlightVisitor.Companion.SQL_TYPE_NAME
import app.cash.sqldelight.intellij.lang.SqlDelightHighlighter
import app.cash.sqldelight.intellij.lang.SqlDelightHighlighter.Companion.SQLITE_COMMA
import app.cash.sqldelight.intellij.lang.SqlDelightHighlighter.Companion.SQLITE_DOC
import app.cash.sqldelight.intellij.lang.SqlDelightHighlighter.Companion.SQLITE_DOT
import app.cash.sqldelight.intellij.lang.SqlDelightHighlighter.Companion.SQLITE_KEYWORD
import app.cash.sqldelight.intellij.lang.SqlDelightHighlighter.Companion.SQLITE_LINE_COMMENT
import app.cash.sqldelight.intellij.lang.SqlDelightHighlighter.Companion.SQLITE_NUMBER
import app.cash.sqldelight.intellij.lang.SqlDelightHighlighter.Companion.SQLITE_OPERATOR
import app.cash.sqldelight.intellij.lang.SqlDelightHighlighter.Companion.SQLITE_PAREN
import app.cash.sqldelight.intellij.lang.SqlDelightHighlighter.Companion.SQLITE_SEMICOLON
import app.cash.sqldelight.intellij.lang.SqlDelightHighlighter.Companion.SQLITE_STRING
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class SqlDelightColorSettingsPage : ColorSettingsPage {

  private companion object {
    val attributes = mapOf(
      "Column" to SQL_COLUMN_NAME,
      "Comma" to SQLITE_COMMA,
      "Comment" to SQLITE_LINE_COMMENT,
      "Doc" to SQLITE_DOC,
      "Dot" to SQLITE_DOT,
      "Function" to SQL_FUNCTION_NAME,
      "Keyword" to SQLITE_KEYWORD,
      "Number token" to SQLITE_NUMBER,
      "Operator" to SQLITE_OPERATOR,
      "Parentheses" to SQLITE_PAREN,
      "Semicolon" to SQLITE_SEMICOLON,
      "Statement identifier" to SQL_STMT_IDENTIFIER_NAME,
      "String token" to SQLITE_STRING,
      "Table or column alias" to SQL_ALIAS,
      "Table" to SQL_TABLE_NAME,
      "Type" to SQL_TYPE_NAME,
    ).map { AttributesDescriptor(it.key, it.value) }.toTypedArray()

    val additionalTags = mapOf(
      "alias" to SQL_ALIAS,
      "column" to SQL_COLUMN_NAME,
      "function" to SQL_FUNCTION_NAME,
      "stmt_id" to SQL_STMT_IDENTIFIER_NAME,
      "table" to SQL_TABLE_NAME,
      "type" to SQL_TYPE_NAME,
    )
  }

  override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
    return attributes
  }

  override fun getColorDescriptors(): Array<ColorDescriptor> {
    return ColorDescriptor.EMPTY_ARRAY
  }

  override fun getDisplayName(): String = "SQLDelight"

  override fun getIcon(): Icon? {
    return null
  }

  override fun getHighlighter(): SyntaxHighlighter = SqlDelightHighlighter()

  override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> {
    return additionalTags
  }

  override fun getDemoText(): String {
    return """
      |/**
      |* Doc comment
      |*/
      |CREATE TABLE crm.<table>product</table> (
      |  <column>id</column> <type>INTEGER</type> PRIMARY KEY,
      |  <column>title</column> <type>TEXT</type>
      |);
      |
      |-- Line comment
      |INSERT INTO <table>product</table> VALUES (1, 'Product1');
      |
      |<stmt_id>count</stmt_id>:
      |SELECT <function>COUNT</function>(*) FROM crm.<table>product</table>;
      |
      |<stmt_id>selectById</stmt_id>:
      |SELECT <column>id</column> AS <alias>ProductID</alias>, <column>title</column> AS <alias>ProductName</alias>
      |  FROM crm.<table>product</table> WHERE <column>id</column> = ?;
      |""".trimMargin()
  }
}
