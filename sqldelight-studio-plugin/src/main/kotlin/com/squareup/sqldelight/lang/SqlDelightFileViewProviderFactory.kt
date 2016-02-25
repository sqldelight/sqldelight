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
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.generating.TableGenerator
import java.io.File

class SqlDelightFileViewProviderFactory : FileViewProviderFactory {
  override fun createFileViewProvider(virtualFile: VirtualFile, language: Language,
      psiManager: PsiManager, eventSystemEnabled: Boolean): FileViewProvider {
    return SqlDelightFileViewProvider(virtualFile, language, psiManager, eventSystemEnabled)
  }
}

internal class SqlDelightFileViewProvider(virtualFile: VirtualFile, language: Language,
    val psiManager: PsiManager, eventSystemEnabled: Boolean) :
    SingleRootFileViewProvider(psiManager, virtualFile, eventSystemEnabled,
        language, SqliteFileType.INSTANCE) {

  val documentManager = PsiDocumentManager.getInstance(psiManager.project)

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
    val file = getPsiInner(SqliteLanguage.INSTANCE) as SqliteFile
    val tableGenerator: TableGenerator
    try {
      tableGenerator = TableGenerator.create(file)
    } catch (e: SqlitePluginException) {
      // SqlitePluginExceptions at this stage need to be propagated to the annotator.
      file.status = SqliteCompiler.Status(e.originatingElement as PsiElement, e.message,
          SqliteCompiler.Status.Result.FAILURE)
      return
    } catch (ignored: Exception) {
      // Ignoring exceptions here since the syntax highlighter handles language-level errors.
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
