package com.alecstrong.sqlite.android.gradle

import com.alecstrong.sqlite.android.SQLiteParser
import com.alecstrong.sqlite.android.SQLiteParser.Create_table_stmtContext
import com.alecstrong.sqlite.android.SQLiteParser.Sql_stmtContext
import com.alecstrong.sqlite.android.SQLiteParser.Column_defContext
import com.alecstrong.sqlite.android.SQLiteParser.Column_constraintContext
import com.alecstrong.sqlite.android.SQLiteParser.ParseContext
import com.alecstrong.sqlite.android.model.Column
import com.alecstrong.sqlite.android.model.ColumnConstraint
import com.alecstrong.sqlite.android.model.ColumnConstraint.NotNullConstraint
import com.alecstrong.sqlite.android.model.SqlStmt.Replacement
import org.antlr.v4.runtime.ParserRuleContext

class TableGenerator
internal constructor(fileName: String, parseContext: SQLiteParser.ParseContext, projectPath: String)
: com.alecstrong.sqlite.android.TableGenerator<ParserRuleContext, Sql_stmtContext, Create_table_stmtContext, Column_defContext, Column_constraintContext>
(parseContext, parseContext.package_stmt(0)?.name()?.map({ it.text })?.joinToString("."), fileName,
    projectPath) {
  override fun sqlStatementElements(originatingElement: ParserRuleContext) =
      when (originatingElement) {
        is SQLiteParser.ParseContext -> originatingElement.sql_stmt_list(0).sql_stmt();
        else -> emptyList<Sql_stmtContext>()
      }

  override fun tableElement(sqlStatementElement: ParserRuleContext) =
      (sqlStatementElement as? ParseContext)?.sql_stmt_list(0)?.create_table_stmt()

  override fun identifier(sqlStatementElement: Sql_stmtContext) =
      sqlStatementElement.IDENTIFIER().text

  override fun columnElements(tableElement: Create_table_stmtContext) = tableElement.column_def()
  override fun tableName(tableElement: Create_table_stmtContext) = tableElement.table_name().text
  override fun isKeyValue(tableElement: Create_table_stmtContext) =
      tableElement.K_KEY_VALUE() != null

  override fun columnName(columnElement: Column_defContext) = columnElement.column_name().text
  override fun classLiteral(columnElement: Column_defContext) =
      columnElement.type_name().sqlite_class_name()?.STRING_LITERAL()?.text

  override fun typeName(columnElement: Column_defContext) =
      when {
        columnElement.type_name().sqlite_class_name() != null ->
          columnElement.type_name().sqlite_class_name().getChild(0).text
        else -> columnElement.type_name().sqlite_type_name().text
      }

  override fun replacementFor(columnElement: Column_defContext, type: Column.Type) =
      Replacement(columnElement.type_name().start.startIndex,
          columnElement.type_name().stop.stopIndex + 1, type.replacement)

  override fun constraintElements(columnElement: Column_defContext) =
      columnElement.column_constraint()

  override fun constraintFor(constraintElement: Column_constraintContext,
      replacements: List<Replacement>): ColumnConstraint<ParserRuleContext>? =
      when {
        constraintElement.K_NOT() != null -> NotNullConstraint(constraintElement);
        else -> null
      }

  override fun startOffset(sqliteStatementElement: ParserRuleContext) =
      when (sqliteStatementElement) {
        is SQLiteParser.Create_table_stmtContext -> sqliteStatementElement.start.startIndex
        else -> (sqliteStatementElement.getChild(
            sqliteStatementElement.childCount - 1) as ParserRuleContext).start.startIndex
      }

  override fun text(
      sqliteStatementElement: ParserRuleContext) = sqliteStatementElement.textWithWhitespace()
}
