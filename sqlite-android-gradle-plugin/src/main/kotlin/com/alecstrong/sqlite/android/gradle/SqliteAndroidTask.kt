package com.alecstrong.sqlite.android.gradle

import com.alecstrong.sqlite.android.SQLiteLexer
import com.alecstrong.sqlite.android.SQLiteParser
import com.alecstrong.sqlite.android.SqliteCompiler
import com.alecstrong.sqlite.android.SqlitePluginException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.StringTokenizer
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.apache.commons.lang3.StringUtils
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails

open class SqliteAndroidTask : SourceTask() {
  private val sqliteCompiler = SqliteCompiler<ParserRuleContext>()

  @get:OutputDirectory var outputDirectory: File? = null
  var buildDirectory: File? = null
    set(value) {
      field = value
      outputDirectory = File(buildDirectory, SqliteCompiler.getOutputDirectory())
    }

  @TaskAction
  fun execute(inputs: IncrementalTaskInputs) {
    inputs.outOfDate { inputFileDetails ->
      if (!inputFileDetails.file.isDirectory) {
        try {
          val errorListener = ErrorListener(inputFileDetails)
          FileInputStream(inputFileDetails.file).use { inputStream ->
            val lexer = SQLiteLexer(ANTLRInputStream(inputStream))
            lexer.removeErrorListeners()
            lexer.addErrorListener(errorListener)

            val parser = SQLiteParser(CommonTokenStream(lexer))
            parser.removeErrorListeners()
            parser.addErrorListener(errorListener)

            val tableGenerator = TableGenerator.create(inputFileDetails.file.name,
                parser.parse(), buildDirectory!!.parent + "/")
            val status = sqliteCompiler.write(tableGenerator)
            if (status.result == SqliteCompiler.Status.Result.FAILURE) {
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

  private fun SqliteCompiler.Status<ParserRuleContext>.message(
      inputFileDetails: InputFileDetails): String {
    val message = StringBuilder(inputFileDetails.file.name)
    if (originatingElement != null) {
      message.append(
          " line ${originatingElement.start.line}:${originatingElement.start.charPositionInLine}")
    }
    message.append(" - $errorMessage\n")
    if (originatingElement != null) {
      message.append(detailText(originatingElement))
    }
    return message.toString()
  }

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
        result.append(("\t\t%" + (maxDigits + start) + "s%s\n").format("",
            StringUtils.repeat('^', element.stop.charPositionInLine - start + 1)))
      }
    }

    return result.toString()
  }

  private fun context(element: ParserRuleContext?): ParserRuleContext? =
      when (element) {
        null -> element
        is SQLiteParser.Create_table_stmtContext -> element
        is SQLiteParser.Sql_stmtContext -> element
        else -> context(element.getParent())
      }
}
