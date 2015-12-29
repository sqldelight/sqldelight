package com.alecstrong.sqlite.android.gradle

import com.alecstrong.sqlite.android.SQLiteParser
import com.alecstrong.sqlite.android.SQLiteParser.Create_table_stmtContext
import com.alecstrong.sqlite.android.SQLiteParser.Sql_stmtContext
import com.alecstrong.sqlite.android.SQLiteParser.Column_defContext
import com.alecstrong.sqlite.android.SQLiteParser.Column_constraintContext
import com.alecstrong.sqlite.android.SQLiteParser.ParseContext
import com.alecstrong.sqlite.android.model.Column
import com.alecstrong.sqlite.android.model.ColumnConstraint
import com.alecstrong.sqlite.android.model.NotNullConstraint
import com.alecstrong.sqlite.android.model.SqlStmt.Replacement
import com.google.common.base.Joiner
import org.antlr.v4.runtime.ParserRuleContext

class TableGenerator
private constructor(fileName: String, parseContext: SQLiteParser.ParseContext, projectPath: String)
: com.alecstrong.sqlite.android.TableGenerator<ParserRuleContext, Sql_stmtContext, Create_table_stmtContext, Column_defContext, Column_constraintContext>
(parseContext, Joiner.on('.').join(parseContext.package_stmt(0).name().map({ it.text })), fileName,
    projectPath) {
  override fun sqlStatementElements(
      originatingElement: ParserRuleContext): Iterable<Sql_stmtContext> {
    return when (originatingElement) {
      is SQLiteParser.ParseContext -> originatingElement.sql_stmt_list(0).sql_stmt();
      else -> emptyList()
    }
  }

  override fun tableElement(originatingElement: ParserRuleContext): Create_table_stmtContext? =
      (originatingElement as? ParseContext)?.sql_stmt_list(0)?.create_table_stmt()

  override fun identifier(sqlStatementElement: Sql_stmtContext): String? =
      sqlStatementElement.IDENTIFIER()?.text

  override fun columnElements(tableElement: Create_table_stmtContext): Iterable<Column_defContext> =
      tableElement.column_def()

  override fun tableName(tableElement: Create_table_stmtContext): String =
      tableElement.table_name().text

  override fun isKeyValue(tableElement: Create_table_stmtContext): Boolean =
      tableElement.K_KEY_VALUE() != null

  override fun columnName(columnElement: SQLiteParser.Column_defContext): String =
      columnElement.column_name().text

  override fun classLiteral(columnElement: Column_defContext): String? =
      columnElement.type_name().sqlite_class_name()?.STRING_LITERAL()?.text

  override fun typeName(columnElement: Column_defContext): String =
      when {
        columnElement.type_name().sqlite_class_name() != null ->
          columnElement.type_name().sqlite_class_name().getChild(0).text
        else -> columnElement.type_name().sqlite_type_name().text
      }

  override fun replacementFor(columnElement: Column_defContext, type: Column.Type): Replacement =
      Replacement(columnElement.type_name().start.startIndex,
          columnElement.type_name().stop.stopIndex + 1, type.replacement)

  override fun constraintElements(
      columnElement: Column_defContext): Iterable<Column_constraintContext> =
      columnElement.column_constraint()

  override fun constraintFor(constraint: Column_constraintContext,
      replacements: List<Replacement>): ColumnConstraint<ParserRuleContext>? =
      when {
        constraint.K_NOT() != null -> NotNullConstraint(constraint);
        else -> null
      }

  override fun startOffset(sqliteStatementElement: ParserRuleContext): Int =
      when (sqliteStatementElement) {
        is SQLiteParser.Create_table_stmtContext -> sqliteStatementElement.start.startIndex
        else -> (sqliteStatementElement.getChild(
            sqliteStatementElement.childCount - 1) as ParserRuleContext).start.startIndex
      }

  override fun text(context: ParserRuleContext): String = context.textWithWhitespace()

  companion object {
    fun create(fileName: String, parseContext: SQLiteParser.ParseContext?,
        projectPath: String): TableGenerator? =
        when {
          parseContext == null || parseContext.package_stmt(0) == null -> null
          else -> TableGenerator(fileName, parseContext, projectPath)
        }
  }
}
