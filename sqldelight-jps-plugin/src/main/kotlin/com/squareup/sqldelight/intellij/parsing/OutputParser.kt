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
package com.squareup.sqldelight.intellij.parsing

import com.android.ide.common.blame.Message
import com.android.ide.common.blame.SourceFilePosition
import com.android.ide.common.blame.SourcePosition
import com.android.ide.common.blame.parser.PatternAwareOutputParser
import com.android.ide.common.blame.parser.util.OutputLineReader
import com.android.utils.ILogger
import java.io.File

class OutputParser : PatternAwareOutputParser {
  private val firstLineOfError = Regex("(.*\\.sq) line ([0-9]*):([0-9]*) - .*")
  private val errorLine = Regex("  [0-9]+.*")
  private val cursorPositionLine = Regex("   *\\^*")

  override fun parse(message: String, outputReader: OutputLineReader, messages: MutableList<Message>,
      logger: ILogger): Boolean {
    if (!message.matches(firstLineOfError)) return false
    val fullError = StringBuilder(message)
    while (outputReader.hasNextLine()) {
      if (outputReader.peek(0).matches(errorLine)) {
        fullError.append('\n').append(outputReader.readLine())
      } else if (outputReader.peek(0).matches(cursorPositionLine)) {
        outputReader.readLine()
      } else {
        break
      }
    }

    val matches = firstLineOfError.matchEntire(message)!!.groupValues
    messages.add(Message(
        Message.Kind.ERROR,
        fullError.toString(),
        SourceFilePosition(
            File(matches[1]),
            SourcePosition(matches[2].toInt() - 1, matches[3].toInt(), 0)
        )
    ))
    return true
  }
}
