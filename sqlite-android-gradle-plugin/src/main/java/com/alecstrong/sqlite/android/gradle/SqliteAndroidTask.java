package com.alecstrong.sqlite.android.gradle;

import com.alecstrong.sqlite.android.SQLiteLexer;
import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.SqliteCompiler;
import com.alecstrong.sqlite.android.model.Table;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import java.io.File;
import java.io.FileInputStream;

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
      try (FileInputStream inputStream = new FileInputStream(inputFileDetails.getFile())) {
        SQLiteLexer lexer = new SQLiteLexer(new ANTLRInputStream(inputStream));
        TokenStream tokenStream = new CommonTokenStream(lexer);
        SQLiteParser parser = new SQLiteParser(tokenStream);

        TableGenerator tableGenerator =
            new TableGenerator(inputFileDetails.getFile().getName(), parser.parse(),
                buildDirectory.getParent());
        SqliteCompiler.Status<ParserRuleContext> status = sqliteCompiler.write(tableGenerator);
        if (status.result == SqliteCompiler.Status.Result.FAILURE) {
          throw new IllegalStateException(status.errorMessage);
        }
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    });
  }
}
