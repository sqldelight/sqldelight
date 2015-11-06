package com.alecstrong.sqlite.android.gradle;

import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.ColumnConstraint;
import com.alecstrong.sqlite.android.model.NotNullConstraint;
import com.alecstrong.sqlite.android.model.SqlStmt;
import com.alecstrong.sqlite.android.model.SqlStmt.Replacement;
import com.alecstrong.sqlite.android.model.Table;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.misc.Interval;

public class TableGenerator {
  Table<ParserRuleContext> generateTable(String fileName, SQLiteParser.ParseContext parseContext,
      String projectPath) {
    if (!parseContext.error().isEmpty()) {
      throw new IllegalStateException("Error: " + parseContext.error(0).toString());
    }

    Table<ParserRuleContext> table = null;
    List<SqlStmt<ParserRuleContext>> sqlStmts = new ArrayList<>();

    for (SQLiteParser.Sql_stmtContext sqlStatement : parseContext.sql_stmt_list(0).sql_stmt()) {
      List<Replacement> replacements = new ArrayList<>();
      if (sqlStatement.create_table_stmt() != null) {
        table = tableFor(fileName, packageName(parseContext), projectPath,
            sqlStatement.create_table_stmt(), replacements);
      }
      if (sqlStatement.IDENTIFIER() != null) {
        sqlStmts.add(sqlStmtFor(sqlStatement, replacements));
      }
    }

    if (table != null) {
      sqlStmts.forEach(table::addSqlStmt);
    }
    return table;
  }

  private Table<ParserRuleContext> tableFor(String fileName, String packageName, String projectPath,
      SQLiteParser.Create_table_stmtContext createTable, List<Replacement> replacements) {
    Table<ParserRuleContext> table =
        new Table<>(packageName, fileName, createTable.table_name().getText(), createTable,
            projectPath + "/");
    for (SQLiteParser.Column_defContext column : createTable.column_def()) {
      table.addColumn(columnFor(column, replacements));
    }
    return table;
  }

  private Column<ParserRuleContext> columnFor(SQLiteParser.Column_defContext column,
      List<Replacement> replacements) {
    String columnName = column.column_name().getText();
    Column<ParserRuleContext> result;
    if (column.type_name().sqlite_type_name() != null) {
      Column.Type type = Column.Type.valueOf(column.type_name().getText());
      replacements.add(new Replacement(column.type_name().start.getStartIndex(),
          column.type_name().stop.getStopIndex() + 1, type.replacement));
      result = new Column<>(columnName, type, column);
    } else {
      Column.Type type =
          Column.Type.valueOf(column.type_name().sqlite_class_name().getChild(0).getText());
      replacements.add(new Replacement(column.type_name().start.getStartIndex(),
          column.type_name().stop.getStopIndex() + 1, type.replacement));
      result = new Column<>(columnName, type,
          column.type_name().sqlite_class_name().STRING_LITERAL().getText(), column);
    }
    for (SQLiteParser.Column_constraintContext constraintNode : column.column_constraint()) {
      ColumnConstraint<ParserRuleContext> constraint = constraintFor(constraintNode, replacements);
      if (constraint != null) result.addConstraint(constraint);
    }
    return result;
  }

  private ColumnConstraint<ParserRuleContext> constraintFor(
      SQLiteParser.Column_constraintContext constraint, List<Replacement> replacements) {
    if (constraint.K_NOT() != null) {
      return new NotNullConstraint<>(constraint);
    }
    return null;
  }

  private SqlStmt<ParserRuleContext> sqlStmtFor(SQLiteParser.Sql_stmtContext sqlStatementParent,
      List<Replacement> replacements) {
    ParserRuleContext sqlStatement =
        (ParserRuleContext) sqlStatementParent.getChild(sqlStatementParent.getChildCount() - 1);
    return new SqlStmt<>(sqlStatementParent.IDENTIFIER().getText(), getFullText(sqlStatement),
        sqlStatement.start.getStartIndex(), replacements, sqlStatement);
  }

  private String packageName(SQLiteParser.ParseContext parseContext) {
    return Joiner.on('.')
        .join(parseContext.package_stmt(0).name().stream().map(RuleContext::getText).iterator());
  }

  private String getFullText(ParserRuleContext context) {
    if (context.start == null
        || context.stop == null
        || context.start.getStartIndex() < 0
        || context.stop.getStopIndex() < 0) {
      return context.getText(); // Fallback
    }

    return context.start.getInputStream()
        .getText(new Interval(context.start.getStartIndex(), context.stop.getStopIndex()));
  }
}
