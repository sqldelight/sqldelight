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

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.squareup.sqldelight.SqlDelightStartupActivity
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteLexer
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.Status
import com.squareup.sqldelight.model.relativePath
import com.squareup.sqldelight.types.SymbolTable
import com.squareup.sqldelight.validation.SqlDelightValidator
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import java.io.File

class SqlDelightFileViewProviderFactory : FileViewProviderFactory {
  override fun createFileViewProvider(virtualFile: VirtualFile, language: Language,
      psiManager: PsiManager, eventSystemEnabled: Boolean): FileViewProvider {
    return SqlDelightFileViewProvider(virtualFile, language, psiManager, eventSystemEnabled)
  }
}

internal class SqlDelightFileViewProvider(virtualFile: VirtualFile, language: Language,
    val psiManager: PsiManager, eventSystemEnabled: Boolean) :
    SingleRootFileViewProvider(psiManager, virtualFile, eventSystemEnabled, language) {

  val documentManager = PsiDocumentManager.getInstance(psiManager.project)
  val file: SqliteFile by lazy {
    getPsiInner(SqliteLanguage.INSTANCE) as SqliteFile
  }

  init {
    val connection = ApplicationManager.getApplication().messageBus.connect()


    ApplicationManager.getApplication().runReadAction {
      file.parseThen { parsed ->
        symbolTable = symbolTable.merge(SymbolTable(parsed), virtualFile)
      }
    }

    connection.subscribe(SqlDelightStartupActivity.TOPIC,
        object : SqlDelightStartupActivity.SqlDelightStartupListener {
          override fun startupCompleted(project: Project) {
            if (project != file.project) return
            ApplicationManager.getApplication().executeOnPooledThread {
              WriteCommandAction.runWriteCommandAction(project, { generateJavaInterface() })
            }
            connection.disconnect()
          }
        })
  }

  override fun contentsSynchronized() {
    super.contentsSynchronized()
    documentManager.performWhenAllCommitted { generateJavaInterface() }
  }

  private fun generateJavaInterface() {
    file.parseThen { parsed ->
      symbolTable = symbolTable.merge(SymbolTable(parsed), virtualFile)
      sqdelightValidator.validate(parsed, symbolTable)

      val status = sqliteCompiler.write(
          parsed,
          file.virtualFile.nameWithoutExtension,
          file.virtualFile.getPlatformSpecificPath().relativePath(parsed),
          ModuleUtil.findModuleForPsiElement(file)!!.moduleFile!!.parent.getPlatformSpecificPath() + File.separatorChar,
          symbolTable
      )

      file.status = status
      if (status is Status.Success) {
        val generatedFile = localFileSystem.findFileByIoFile(status.generatedFile)
        if (generatedFile != file.generatedFile?.virtualFile) {
          file.generatedFile?.delete()
        }
        file.generatedFile = psiManager.findFile(generatedFile ?: return@parseThen)
      }
    }
  }

  companion object {
    private val localFileSystem = LocalFileSystem.getInstance()
    private val sqliteCompiler = SqliteCompiler()
    private val sqdelightValidator = SqlDelightValidator()

    private var symbolTable = SymbolTable()

    private fun SqliteFile.parseThen(operation: (SqliteParser.ParseContext) -> Unit) {
      synchronized (sqliteCompiler) {
        val errorListener = GeneratingErrorListener()
        val lexer = SqliteLexer(ANTLRInputStream(text))
        lexer.removeErrorListeners()
        lexer.addErrorListener(errorListener)

        val parser = SqliteParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(errorListener)

        val parsed = parser.parse()

        if (errorListener.hasError) {
          // Syntax level errors are handled by the annotator. Don't generate anything.
          return
        }

        try {
          operation(parsed)
        } catch (e: SqlitePluginException) {
          status = Status.Failure(e.originatingElement, e.message)
        }
      }
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

internal fun VirtualFile.getPlatformSpecificPath() = path.replace('/', File.separatorChar)
