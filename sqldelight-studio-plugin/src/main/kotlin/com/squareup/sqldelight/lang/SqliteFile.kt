/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.squareup.sqldelight.SqliteLexer
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.Status
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

class SqliteFile internal constructor(viewProvider: FileViewProvider)
: PsiFileBase(viewProvider, SqliteLanguage.INSTANCE) {
  var generatedFile: PsiFile? = null
  var status: Status? = null

  override fun getFileType() = SqliteFileType.INSTANCE
  override fun toString() = "SQLite file"

  fun parseThen(
      operation: (com.squareup.sqldelight.SqliteParser.ParseContext) -> Unit,
      onError: () -> Unit = { /* no op */ }
  ) {
    synchronized (project) {
      val errorListener = GeneratingErrorListener()
      val lexer = SqliteLexer(ANTLRInputStream(text))
      lexer.removeErrorListeners()
      lexer.addErrorListener(errorListener)

      val parser = com.squareup.sqldelight.SqliteParser(CommonTokenStream(lexer))
      parser.removeErrorListeners()
      parser.addErrorListener(errorListener)

      val parsed = parser.parse()

      if (errorListener.hasError) {
        // Syntax level errors are handled by the annotator. Don't generate anything.
        onError()
        return
      }

      try {
        operation(parsed)
      } catch (e: SqlitePluginException) {
        status = Status.Failure(e.originatingElement, e.message)
      }
    }
  }

  private class GeneratingErrorListener : BaseErrorListener() {
    internal var hasError = false

    override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int,
        charPositionInLine: Int, msg: String?, e: RecognitionException?) {
      hasError = true
    }
  }
}
