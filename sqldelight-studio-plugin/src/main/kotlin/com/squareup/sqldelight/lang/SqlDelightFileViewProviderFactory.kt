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
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteLexer
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.TableGenerator
import com.squareup.sqldelight.relativePath
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
    ApplicationManager.getApplication().executeOnPooledThread {
      WriteCommandAction.runWriteCommandAction(psiManager.project, { generateJavaInterface() })
    }
  }

  override fun contentsSynchronized() {
    super.contentsSynchronized()
    documentManager.performWhenAllCommitted { generateJavaInterface() }
  }

  private fun generateJavaInterface() {
    val errorListener = GeneratingErrorListener()
    val lexer = SqliteLexer(ANTLRInputStream(file.text))
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

    val tableGenerator: TableGenerator
    try {
      tableGenerator = TableGenerator(parsed,
          file.virtualFile.path.relativePath(parsed),
          ModuleUtil.findModuleForPsiElement(file)!!.moduleFile!!.parent.path + File.separatorChar)
    } catch (e: SqlitePluginException) {
      // Generation level error. Propogate to the annotator.
      file.status = SqliteCompiler.Status(e.originatingElement, e.message,
          SqliteCompiler.Status.Result.FAILURE)
      return
    }

    file.status = sqliteCompiler.write(tableGenerator)
    val outputDirectory = localFileSystem.findFileByIoFile(tableGenerator.outputDirectory)
    outputDirectory?.refresh(true, true)
    val generatedFile = outputDirectory?.findFileByRelativePath(
        "${tableGenerator.packageDirectory}${File.separatorChar}${tableGenerator.generatedFileName}.java")
    if (generatedFile != file.generatedFile?.virtualFile) {
      file.generatedFile?.delete()
    }
    file.generatedFile = psiManager.findFile(generatedFile ?: return)
  }

  companion object {
    private val localFileSystem = LocalFileSystem.getInstance()
    private val sqliteCompiler = SqliteCompiler<PsiElement>()
  }
}

private class GeneratingErrorListener : BaseErrorListener() {
  internal var hasError = false

  override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int,
      charPositionInLine: Int, msg: String?, e: RecognitionException?) {
    hasError = true
  }
}
