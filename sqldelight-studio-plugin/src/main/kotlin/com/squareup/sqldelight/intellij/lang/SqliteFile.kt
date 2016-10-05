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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteLexer
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.Status
import com.squareup.sqldelight.intellij.SqlDelightManager
import com.squareup.sqldelight.intellij.util.elementAt
import com.squareup.sqldelight.intellij.util.getOrCreateFile
import com.squareup.sqldelight.intellij.util.getOrCreateSubdirectory
import com.squareup.sqldelight.model.relativePath
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import java.io.File
import kotlin.reflect.KProperty

class SqliteFile internal constructor(
    viewProvider: FileViewProvider, moduleDir: VirtualFile
) : PsiFileBase(viewProvider, SqliteLanguage.INSTANCE) {
  private val vfileDelegate = VirtualFileDelegate(viewProvider, moduleDir, PsiManager.getInstance(project))

  internal val relativePath: String
    get() = viewProvider.virtualFile.path.relativePath('/').joinToString(File.separator)
  internal val generatedVirtualFile: VirtualFile by vfileDelegate
  internal val generatedPsiFile: PsiFile?
    get() = vfileDelegate.psiFile
  internal val generatedDocument: Document
    get() = fileDocumentManager.getDocument(generatedVirtualFile)!!
  internal var status: Status? = null
  internal var dirty = true

  private lateinit var parsed: SqliteParser.ParseContext

  override fun getFileType() = SqliteFileType.INSTANCE
  override fun toString() = "SQLite file"

  fun parseThen(
      operation: (SqliteParser.ParseContext) -> Unit,
      onError: (SqliteParser.ParseContext, List<Token>) -> Unit = { parsed, errors -> /* no op */ }
  ) {
    val manager = SqlDelightManager.getInstance(this) ?: return

    synchronized (manager) {
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
      this.parsed = parsed
      manager.setParseTree(this, parsed)

      try {
        if (errorListener.errors.isNotEmpty()) {
          onError(parsed, errorListener.errors)
        } else {
          operation(parsed)
        }
      } catch (e: SqlitePluginException) {
        status = Status.Failure(e.originatingElement, e.message)
      }
    }
  }

  internal fun elementAt(offset: Int): ParserRuleContext? {
    dirty = true
    var element: ParserRuleContext? = null
    parseThen(
        { element = it.elementAt(offset) },
        { parsed, errors -> element = parsed.elementAt(offset)}
    )
    return element
  }

  fun write(text: String) {
    // Dont generate java in tests. Maybe later. Right now it gives me headaches.
    if (ApplicationManager.getApplication().isUnitTestMode) return
    ApplicationManager.getApplication().invokeLater {
      ApplicationManager.getApplication().runWriteAction {
        val document = generatedDocument
        document.setText(text)
        document.createGuardedBlock(0, document.textLength)
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
    private val fileDocumentManager = FileDocumentManager.getInstance()
  }

  /**
   * There are a few situations where we need to create the file ourselves. The initial creation
   * of the virtual file is done lazily, and subsequent calls to get the virtual file first
   * check if it is valid, and if not recreate the virtual file.
   */
  class VirtualFileDelegate(
      val viewProvider: FileViewProvider,
      val moduleDir: VirtualFile,
      val psiManager: PsiManager
  ) {
    val applicationManager = ApplicationManager.getApplication()
    var backingFile: VirtualFile? = null
    val psiFile: PsiFile?
      get() = backingFile?.let { if (it.isValid) psiManager.findFile(it) else null }

    operator fun getValue(thisRef: SqliteFile, property: KProperty<*>): VirtualFile {
      applicationManager.assertWriteAccessAllowed()
      synchronized(this) {
        val backingFile = this.backingFile
        if (backingFile == null || !backingFile.isValid) {
          val modulePsi = psiManager.findDirectory(moduleDir)!!
          val vfile = viewProvider.virtualFile
          val psiFile = (SqliteCompiler.OUTPUT_DIRECTORY + vfile.path.relativePath('/').dropLast(1)).fold(
              modulePsi.getOrCreateSubdirectory("build"),
              { directory, childDirName -> directory.getOrCreateSubdirectory(childDirName) }
          ).getOrCreateFile("${SqliteCompiler.interfaceName(vfile.nameWithoutExtension)}.java")
          this.backingFile = psiFile.virtualFile
          return psiFile.virtualFile
        } else {
          return backingFile
        }
      }
    }
  }
}
