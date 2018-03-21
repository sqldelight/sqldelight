/*
 * Copyright (C) 2017 Square, Inc.
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
package com.squareup.sqldelight.core

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.alecstrong.sqlite.psi.core.SqliteCoreEnvironment
import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.alecstrong.sqlite.psi.core.psi.SqliteSqlStmt
import com.intellij.mock.MockModule
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.testFramework.registerServiceInstance
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.core.lang.SqlDelightParserDefinition
import com.squareup.sqldelight.core.psi.SqlDelightImportStmt
import java.io.File
import java.util.ArrayList
import java.util.StringTokenizer

/**
 * Mocks an intellij environment for compiling sqldelight files without an instance of intellij
 * running.
 */
class SqlDelightEnvironment(
    /**
     * The sqlite source directories for this environment.
     */
    private val sourceFolders: List<File>,
    /**
     * The package name to be used for the generated SqlDelightDatabase class.
     */
    packageName: String,
    /**
     * An output directory to place the generated class files.
     */
    private val outputDirectory: File
) : SqliteCoreEnvironment(SqlDelightParserDefinition(), SqlDelightFileType, sourceFolders),
    SqlDelightProjectService {
  val project: Project = projectEnvironment.project
  val module = MockModule(project, project)

  init {
    module.registerService(SqlDelightFileIndex::class.java, FileIndex(packageName))
    project.registerServiceInstance(SqlDelightProjectService::class.java, this)
  }

  override fun module(vFile: VirtualFile) = module

  /**
   * Run the SQLDelight compiler and return the error or success status.
   */
  fun generateSqlDelightFiles(): CompilationStatus {
    val errors = ArrayList<String>()
    annotate(object : SqliteAnnotationHolder {
      override fun createErrorAnnotation(element: PsiElement, s: String) {
        errors.add(errorMessage(element, s))
      }
    })
    if (errors.isNotEmpty()) return CompilationStatus.Failure(errors)

    forSourceFiles {
       SqlDelightCompiler.compile(module, it as SqlDelightFile) { fileName ->
        val file = File(outputDirectory, fileName)
        if (!file.exists()) {
          file.parentFile.mkdirs()
          file.createNewFile()
        }
        return@compile file.writer()
      }
    }

    return CompilationStatus.Success()
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

  sealed class CompilationStatus {
    class Success: CompilationStatus()
    class Failure(val errors: List<String>): CompilationStatus()
  }

  private inner class FileIndex(override val packageName: String): SqlDelightFileIndex {
    override val isConfigured = true

    override val outputDirectory = this@SqlDelightEnvironment.outputDirectory.absolutePath

    private val directories: List<PsiDirectory> by lazy {
      val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
      val psiManager = PsiManager.getInstance(projectEnvironment.project)
      return@lazy sourceFolders
          .map { localFileSystem.findFileByPath(it.absolutePath)!! }
          .map { psiManager.findDirectory(it)!! }
    }

    override fun packageName(file: SqlDelightFile): String {
      fun PsiFileSystemItem.relativePathUnder(ancestor: PsiDirectory): List<String>? {
        if (this.virtualFile.path == ancestor.virtualFile.path) return emptyList()
        parent?.let {
          return it.relativePathUnder(ancestor)?.plus(name)
        }
        return null
      }

      for (sourceFolder in sourceFolders()) {
        val path = file.parent!!.relativePathUnder(sourceFolder)
        if (path != null) return path.joinToString(separator = ".")
      }

      throw IllegalStateException("Tried to find package name of file ${file.virtualFile.path} when" +
          " it is not under any of the source folders $sourceFolders")
    }

    override fun sourceFolders(file: SqlDelightFile?) = directories
  }
}

