package com.alecstrong.sqlite.android.gradle

import com.alecstrong.sqlite.android.SqlitePluginException
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.gradle.api.tasks.incremental.InputFileDetails

class ErrorListener(private val inputFileDetails: InputFileDetails) : BaseErrorListener() {
  override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any, line: Int,
      charPositionInLine: Int, msg: String, e: RecognitionException?) {
    throw SqlitePluginException(offendingSymbol, "${inputFileDetails.file.name} line $line:$charPositionInLine - $msg")
  }
}
