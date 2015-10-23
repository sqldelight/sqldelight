package com.alecstrong.sqlite.android.gradle;

import com.alecstrong.sqlite.android.SQLiteLexer;
import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.SqliteCompiler;
import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.SqlStmt;
import com.alecstrong.sqlite.android.model.Table;
import com.google.common.base.Joiner;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.Interval;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

public class SqliteAndroidTask extends SourceTask {
  private final File outputDirectory = new File("build/generated-src");

  @OutputDirectory
  public File getOutputDirectory() {
    return outputDirectory;
  }

  @TaskAction
  public void execute(IncrementalTaskInputs inputs) {
    inputs.outOfDate(inputFileDetails -> {
      try (FileInputStream inputStream = new FileInputStream(inputFileDetails.getFile())) {
        SQLiteLexer lexer = new SQLiteLexer(new ANTLRInputStream(inputStream));
        TokenStream tokenStream = new CommonTokenStream(lexer);
        SQLiteParser parser = new SQLiteParser(tokenStream);

        Table table = tableFor(parser.parse());
        if (table != null) {
          SqliteCompiler.write(table, outputDirectory);
        }
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    });
  }

  private static Table tableFor(SQLiteParser.ParseContext parseContext) {
    if (!parseContext.error().isEmpty()) {
      throw new IllegalStateException("Error: " + parseContext.error(0).toString());
    }
    String packageName = Joiner.on('.')
        .join(parseContext.package_stmt(0).name().stream().map(name -> name.getText()).iterator());
    List<SQLiteParser.Sql_stmtContext> sqlStatements = parseContext.sql_stmt_list(0).sql_stmt();

    Table table = null;
    List<SqlStmt> sqlStmts = new ArrayList<>();
    for (SQLiteParser.Sql_stmtContext sqlStatement : sqlStatements) {
      if (sqlStatement.IDENTIFIER() != null) {
        sqlStmts.add(new SqlStmt(sqlStatement.IDENTIFIER().getText(), getFullText(
            (ParserRuleContext) sqlStatement.getChild(sqlStatement.getChildCount() - 1))));
      }
      if (sqlStatement.create_table_stmt() != null) {
        SQLiteParser.Create_table_stmtContext createTable = sqlStatement.create_table_stmt();
        table = new Table(packageName, createTable.table_name().getText());
        for (SQLiteParser.Column_defContext column : createTable.column_def()) {
          String columnName = column.column_name().getText();
          Column.Type type = Column.Type.valueOf(column.type_name().getText());
          table.addColumn(new Column(columnName, type));
        }
      }
    }
    if (table != null) {
      sqlStmts.forEach(table::addSqlStmt);
    }
    return table;
  }

  public static String getFullText(ParserRuleContext context) {
    if (context.start == null
        || context.stop == null
        || context.start.getStartIndex() < 0
        || context.stop.getStopIndex() < 0) {
      return context.getText(); // Fallback
    }

    return context.start.getInputStream().getText(
        Interval.of(context.start.getStartIndex(), context.stop.getStopIndex()));
  }
}
