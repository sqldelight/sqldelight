package com.alecstrong.sqlite.android.gradle;

import com.alecstrong.sqlite.android.SqlitePluginException;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.gradle.api.tasks.incremental.InputFileDetails;

public class ErrorListener extends BaseErrorListener {
  private final InputFileDetails inputFileDetails;

  public ErrorListener(InputFileDetails inputFileDetails) {
    this.inputFileDetails = inputFileDetails;
  }

  @Override public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
      int charPositionInLine, String msg, RecognitionException e) {
    throw new SqlitePluginException(offendingSymbol, String.format("%s line %d:%d - %s",
        inputFileDetails.getFile().getName(), line, charPositionInLine, msg));
  }
}
