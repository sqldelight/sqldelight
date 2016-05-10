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
package com.squareup.sqldelight.intellij.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.util.containers.BidirectionalMap
import com.squareup.sqldelight.SqliteLexer
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.Status
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token

class SqliteFile internal constructor(viewProvider: FileViewProvider)
: PsiFileBase(viewProvider, SqliteLanguage.INSTANCE) {
  internal var generatedFile: PsiFile? = null
  internal var status: Status? = null
  internal var dirty = true;

  private lateinit var parsed: SqliteParser.ParseContext

  override fun getFileType() = SqliteFileType.INSTANCE
  override fun toString() = "SQLite file"

  fun parseThen(
      operation: (SqliteParser.ParseContext) -> Unit,
      onError: (SqliteParser.ParseContext, List<Token>) -> Unit = { parsed, errors -> /* no op */ }
  ) {
    synchronized (project) {
      if (!dirty) {
        operation(parsed)
        return@synchronized
      }
      dirty = false
      val errorListener = GeneratingErrorListener()
      val lexer = SqliteLexer(ANTLRInputStream(text))
      lexer.removeErrorListeners()
      lexer.addErrorListener(errorListener)

      val parser = com.squareup.sqldelight.SqliteParser(CommonTokenStream(lexer))
      parser.removeErrorListeners()
      parser.addErrorListener(errorListener)

      val parsed = parser.parse()
      this.parsed = parsed;
      parseTreeMap.remove(this)
      parseTreeMap.put(this, parsed)

      if (errorListener.errors.isNotEmpty()) {
        // Syntax level errors are handled by the annotator. Don't generate anything.
        onError(parsed, errorListener.errors)
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
    internal val errors = arrayListOf<Token>()

    override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int,
        charPositionInLine: Int, msg: String?, e: RecognitionException?) {
      errors.add(offendingSymbol as Token)
    }
  }

  companion object {
    internal val parseTreeMap = BidirectionalMap<SqliteFile, SqliteParser.ParseContext>()
  }
}
