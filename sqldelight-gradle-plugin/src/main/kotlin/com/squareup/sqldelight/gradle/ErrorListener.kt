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
package com.squareup.sqldelight.gradle

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import java.io.File

class ErrorListener(private val file: File) : BaseErrorListener() {
  override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any, line: Int,
      charPositionInLine: Int, msg: String, e: RecognitionException?) {
    throw SqliteSyntaxException("${file.name} line $line:$charPositionInLine - $msg")
  }
}

private class SqliteSyntaxException(message: String) : IllegalStateException(message)
