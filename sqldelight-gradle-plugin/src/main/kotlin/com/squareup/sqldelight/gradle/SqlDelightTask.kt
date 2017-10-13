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

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.alecstrong.sqlite.psi.core.SqliteCoreEnvironment
import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteSqlStmt
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.core.lang.SqlDelightParserDefinition
import com.squareup.sqldelight.core.psi.SqlDelightImportStmt
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.StringTokenizer

open class SqlDelightTask : SourceTask() {
  private val parser = SqlDelightParserDefinition()

  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input fun pluginVersion() = VERSION

  @get:OutputDirectory var outputDirectory: File? = null

  var buildDirectory: File? = null
    set(value) {
      field = value
      outputDirectory = listOf("generated", "source", "sqldelight").fold(value, ::File)
    }

  @TaskAction
  fun generateSqlDelightFiles() {
    val environment = SqliteCoreEnvironment(parser, SqlDelightFileType, getSource().asPath)
    var hasError = false
    environment.annotate(object : SqliteAnnotationHolder {
      override fun createErrorAnnotation(element: PsiElement, s: String?) {
        if (!hasError) logger.log(LogLevel.ERROR, "")
        hasError = true
        logger.log(LogLevel.ERROR, errorMessage(element, s!!))
      }
    })
    if (hasError) throw SqlDelightException(
        "Generation failed; see the generator error output for details.")

    environment.forSourceFiles {
      SqlDelightCompiler.compile(it as SqlDelightFile) { fileName ->
        val file = File(outputDirectory, fileName)
        if (!file.exists()) {
          file.parentFile.mkdirs()
          file.createNewFile()
        }
        return@compile file.writer()
      }
    }
  }

  private fun errorMessage(element: PsiElement, message: String): String {
    return "${element.containingFile.virtualFile.path} " +
        "line ${element.lineStart}:${element.charPositionInLine} - $message\n${detailText(element)}"
  }

  private fun detailText(element: PsiElement) = try {
    val context = context(element) ?: element
    val result = StringBuilder()
    val tokenizer = StringTokenizer(context.text, "\n", false)

    val maxDigits = (Math.log10(context.lineStart.toDouble()) + 1).toInt()
    for (line in context.lineStart..context.lineEnd) {
      result.append(("%0${maxDigits}d    %s\n").format(line, tokenizer.nextToken()))
      if (element.lineStart == element.lineEnd && element.lineStart == line) {
        // If its an error on a single line highlight where on the line.
        result.append(("%${maxDigits}s    ").format(""))
        if (element.charPositionInLine > 0) {
          result.append(("%${element.charPositionInLine}s").format(""))
        }
        result.append(("%s\n").format("^".repeat(element.textLength)))
      }
    }

    result.toString()
  } catch (e: Exception) {
    // If there is an exception while trying to print an error, just give back the unformatted error
    // and print the stack trace for more debugging.
    e.printStackTrace()
    element.text
  }

  private val PsiElement.charPositionInLine: Int
    get() {
      val file = PsiDocumentManager.getInstance(project).getDocument(containingFile)!!
      return textOffset - file.getLineStartOffset(lineStart)
    }

  private val PsiElement.lineStart: Int
    get() {
      val file = PsiDocumentManager.getInstance(project).getDocument(containingFile)!!
      return file.getLineNumber(textOffset)
    }

  private val PsiElement.lineEnd: Int
    get() {
      val file = PsiDocumentManager.getInstance(project).getDocument(containingFile)!!
      return file.getLineNumber(textOffset + textLength)
    }

  private fun context(element: PsiElement?): PsiElement? =
      when (element) {
        null -> element
        is SqliteCreateTableStmt -> element
        is SqliteSqlStmt -> element
        is SqlDelightImportStmt -> element
        else -> context(element.parent)
      }
}
