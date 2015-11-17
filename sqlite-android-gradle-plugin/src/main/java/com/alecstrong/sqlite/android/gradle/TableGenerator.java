package com.alecstrong.sqlite.android.gradle;

import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.ColumnConstraint;
import com.alecstrong.sqlite.android.model.NotNullConstraint;
import com.alecstrong.sqlite.android.model.SqlStmt.Replacement;
import com.google.common.base.Joiner;
import java.util.Collections;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.misc.Interval;

public class TableGenerator extends
    com.alecstrong.sqlite.android.TableGenerator<ParserRuleContext, SQLiteParser.Sql_stmtContext, SQLiteParser.Create_table_stmtContext, SQLiteParser.Column_defContext, SQLiteParser.Column_constraintContext> {

  public TableGenerator(String fileName, SQLiteParser.ParseContext parseContext,
      String projectPath) {
    super(parseContext, Joiner.on('.')
            .join(
                parseContext.package_stmt(0).name().stream().map(RuleContext::getText).iterator()),
        fileName, projectPath);
  }

  @Override protected Iterable<SQLiteParser.Sql_stmtContext> sqlStatementElements(
      ParserRuleContext originatingElement) {
    if (originatingElement instanceof SQLiteParser.ParseContext) {
      return ((SQLiteParser.ParseContext) originatingElement).sql_stmt_list(0).sql_stmt();
    }
    return Collections.emptyList();
  }

  @Override protected SQLiteParser.Create_table_stmtContext tableElement(
      SQLiteParser.Sql_stmtContext sqlStatementElement) {
    return sqlStatementElement.create_table_stmt();
  }

  @Override protected String identifier(SQLiteParser.Sql_stmtContext sqlStatementElement) {
    return sqlStatementElement.IDENTIFIER().getText();
  }

  @Override protected Iterable<SQLiteParser.Column_defContext> columnElements(
      SQLiteParser.Create_table_stmtContext tableElement) {
    return tableElement.column_def();
  }

  @Override protected String tableName(SQLiteParser.Create_table_stmtContext tableElement) {
    return tableElement.table_name().getText();
  }

  @Override protected boolean isKeyValue(SQLiteParser.Create_table_stmtContext tableElement) {
    return tableElement.K_KEY_VALUE() != null;
  }

  @Override protected String columnName(SQLiteParser.Column_defContext columnElement) {
    return columnElement.column_name().getText();
  }

  @Override protected String classLiteral(SQLiteParser.Column_defContext columnElement) {
    return columnElement.type_name().sqlite_class_name() != null
        ? columnElement.type_name().sqlite_class_name().STRING_LITERAL().getText()
        : null;
  }

  @Override protected String typeName(SQLiteParser.Column_defContext columnElement) {
    return columnElement.type_name().sqlite_class_name() != null
        ? columnElement.type_name().sqlite_class_name().getChild(0).getText()
        : columnElement.type_name().sqlite_type_name().getText();
  }

  @Override
  protected Replacement replacementFor(SQLiteParser.Column_defContext columnElement,
      Column.Type type) {
    return new Replacement(columnElement.type_name().start.getStartIndex(),
        columnElement.type_name().stop.getStopIndex() + 1, type.replacement);
  }

  @Override protected Iterable<SQLiteParser.Column_constraintContext> constraintElements(
      SQLiteParser.Column_defContext columnElement) {
    return columnElement.column_constraint();
  }

  @Override protected ColumnConstraint<ParserRuleContext> constraintFor(
      SQLiteParser.Column_constraintContext constraint, List<Replacement> replacements) {
    if (constraint.K_NOT() != null) {
      return new NotNullConstraint<>(constraint);
    }
    return null;
  }

  @Override protected int startOffset(SQLiteParser.Sql_stmtContext sqliteStatementElement) {
    return ((ParserRuleContext) sqliteStatementElement.getChild(
        sqliteStatementElement.getChildCount() - 1)).start.getStartIndex();
  }

  @Override protected String text(SQLiteParser.Sql_stmtContext context) {
    return text((ParserRuleContext) context.getChild(context.getChildCount() - 1));
  }

  private String text(ParserRuleContext context) {
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
