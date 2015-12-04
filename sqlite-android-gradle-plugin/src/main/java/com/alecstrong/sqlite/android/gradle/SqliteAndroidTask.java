package com.alecstrong.sqlite.android.gradle;

import com.alecstrong.sqlite.android.SQLiteLexer;
import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.SqliteCompiler;
import com.alecstrong.sqlite.android.SqlitePluginException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;

public class SqliteAndroidTask extends SourceTask {
  private final SqliteCompiler<ParserRuleContext> sqliteCompiler = new SqliteCompiler<>();

  private File outputDirectory;
  private File buildDirectory;

  public void setBuildDirectory(File buildDirectory) {
    this.buildDirectory = buildDirectory;
    outputDirectory = new File(buildDirectory, SqliteCompiler.getOutputDirectory());
  }

  @OutputDirectory
  public File getOutputDirectory() {
    return outputDirectory;
  }

  @TaskAction
  public void execute(IncrementalTaskInputs inputs) {
    inputs.outOfDate(inputFileDetails -> {
      if (inputFileDetails.getFile().isDirectory()) return;

      ErrorListener errorListener = new ErrorListener(inputFileDetails);
      try (FileInputStream inputStream = new FileInputStream(inputFileDetails.getFile())) {
        SQLiteLexer lexer = new SQLiteLexer(new ANTLRInputStream(inputStream));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        TokenStream tokenStream = new CommonTokenStream(lexer);
        SQLiteParser parser = new SQLiteParser(tokenStream);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        TableGenerator tableGenerator = TableGenerator.create(inputFileDetails.getFile().getName(),
            parser.parse(), buildDirectory.getParent() + "/");
        SqliteCompiler.Status<ParserRuleContext> status = sqliteCompiler.write(tableGenerator);
        if (status.result == SqliteCompiler.Status.Result.FAILURE) {
          throw new SqlitePluginException(status.originatingElement,
              message(status, inputFileDetails));
        }
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    });
  }

  private String message(SqliteCompiler.Status<ParserRuleContext> status,
      InputFileDetails inputFileDetails) {
    StringBuilder message = new StringBuilder(inputFileDetails.getFile().getName());
    if (status.originatingElement != null) {
      message.append(" line ")
          .append(status.originatingElement.start.getLine())
          .append(':')
          .append(status.originatingElement.start.getCharPositionInLine());
    }
    message.append(" - ")
        .append(status.errorMessage)
        .append('\n');
    if (status.originatingElement != null) {
      message.append(detailText(status.originatingElement));
    }
    return message.toString();
  }

  private String detailText(ParserRuleContext element) {
    ParserRuleContext context = context(element);
    if (context == null) context = element;
    String contextText = TableGenerator.textFor(context);

    StringBuilder result = new StringBuilder();

    StringTokenizer tokenizer = new StringTokenizer(contextText, "\n", false);

    int maxDigits = (int) (Math.log10(context.stop.getLine()) + 1);
    for (int line = context.start.getLine(); line <= context.stop.getLine(); line++) {
      result.append(String.format("%0" + maxDigits + "d\t\t%s\n", line, tokenizer.nextToken()));
      if (element.start.getLine() == element.stop.getLine() && element.start.getLine() == line) {
        // If its an error on a single line highlight where on the line.
        int start = element.start.getCharPositionInLine();
        result.append(String.format("\t\t%" + (maxDigits + start) + "s%s\n", "",
            StringUtils.repeat('^', element.stop.getCharPositionInLine() - start + 1)));
      }
    }

    return result.toString();
  }

  private ParserRuleContext context(ParserRuleContext element) {
    if (element instanceof SQLiteParser.Create_table_stmtContext
        || element instanceof SQLiteParser.Sql_stmtContext) {
      return element;
    } else if (element.getParent() == null) {
      return null;
    }
    return context(element.getParent());
  }
}
