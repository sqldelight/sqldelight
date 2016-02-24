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

import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteCompiler.Companion
import com.squareup.sqldelight.SqliteCompiler.Status
import com.squareup.sqldelight.SqliteCompiler.Status.Result.FAILURE
import com.squareup.sqldelight.SqliteLexer
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.SqliteParser.Create_table_stmtContext
import com.squareup.sqldelight.SqliteParser.Sql_stmtContext
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.relativePath
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.apache.commons.lang3.StringUtils
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.StringTokenizer

open class SqlDelightTask : SourceTask() {
  private val sqliteCompiler = SqliteCompiler<ParserRuleContext>()

  @get:OutputDirectory var outputDirectory: File? = null
  var buildDirectory: File? = null
    set(value) {
      field = value
      outputDirectory = File(buildDirectory, Companion.OUTPUT_DIRECTORY)
    }

  @TaskAction
  fun execute(inputs: IncrementalTaskInputs) {
    inputs.outOfDate { inputFileDetails ->
      if (!inputFileDetails.file.isDirectory) {
        try {
          val errorListener = ErrorListener(inputFileDetails)
          FileInputStream(inputFileDetails.file).use { inputStream ->
            val lexer = SqliteLexer(ANTLRInputStream(inputStream))
            lexer.removeErrorListeners()
            lexer.addErrorListener(errorListener)

            val parser = SqliteParser(CommonTokenStream(lexer))
            parser.removeErrorListeners()
            parser.addErrorListener(errorListener)

            val tableGenerator = TableGenerator(inputFileDetails.file.absolutePath.relativePath(),
                parser.parse(), buildDirectory!!.parent + File.separatorChar)
            val status = sqliteCompiler.write(tableGenerator)
            if (status.result == FAILURE) {
              throw SqlitePluginException(status.originatingElement,
                  status.message(inputFileDetails))
            }
          }
        } catch (e: IOException) {
          throw IllegalStateException(e)
        }
      }
    }
  }

  private fun Status<ParserRuleContext>.message(inputFileDetails: InputFileDetails) = "" +
      "${inputFileDetails.file.name} " +
      "line ${originatingElement.start.line}:${originatingElement.start.charPositionInLine}" +
      " - $errorMessage\n${detailText(originatingElement)}"

  private fun detailText(element: ParserRuleContext): String {
    val context = context(element) ?: element
    val result = StringBuilder()
    val tokenizer = StringTokenizer(context.textWithWhitespace(), "\n", false)

    val maxDigits = (Math.log10(context.stop.line.toDouble()) + 1).toInt()
    for (line in context.start.line..context.stop.line) {
      result.append(("%0${maxDigits}d\t\t%s\n").format(line, tokenizer.nextToken()))
      if (element.start.line == element.stop.line && element.start.line == line) {
        // If its an error on a single line highlight where on the line.
        val start = element.start.charPositionInLine
        result.append(("%${maxDigits}s\t\t%${start}s%s\n").format("", "",
            StringUtils.repeat('^', element.stop.charPositionInLine - start + 1)))
      }
    }

    return result.toString()
  }

  private fun context(element: ParserRuleContext?): ParserRuleContext? =
      when (element) {
        null -> element
        is Create_table_stmtContext -> element
        is Sql_stmtContext -> element
        else -> context(element.getParent())
      }
}
