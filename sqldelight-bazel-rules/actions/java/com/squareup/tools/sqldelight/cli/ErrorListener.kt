package com.squareup.tools.sqldelight.cli

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import java.io.File

class ErrorListener(private val file: File) : BaseErrorListener() {
  override fun syntaxError(
    recognizer: Recognizer<*, *>?,
    offendingSymbol: Any,
    line: Int,
    charPositionInLine: Int,
    msg: String,
    e: RecognitionException?
  ) {
    throw SqlDelightException("${file.name} line $line:$charPositionInLine - $msg")
  }
}
