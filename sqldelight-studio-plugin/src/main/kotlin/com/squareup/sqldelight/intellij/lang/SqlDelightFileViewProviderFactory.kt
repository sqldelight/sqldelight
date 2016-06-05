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

import com.intellij.lang.Language
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.Status
import com.squareup.sqldelight.Status.ValidationStatus.Invalid
import com.squareup.sqldelight.intellij.SqlDelightManager
import com.squareup.sqldelight.model.moduleDirectory
import com.squareup.sqldelight.model.relativePath
import com.squareup.sqldelight.types.SymbolTable
import com.squareup.sqldelight.validation.SqlDelightValidator
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

  override fun contentsSynchronized() {
    super.contentsSynchronized()
    documentManager.performWhenAllCommitted { generateJavaInterface(true) }
  }

  internal fun generateJavaInterface(fromEdit: Boolean = false) {
    val manager = SqlDelightManager.getInstance(file) ?: return

    // Mark the file as dirty and re-parse.
    file.dirty = true
    file.parseThen({ parsed ->
      manager.symbolTable += SymbolTable(parsed, virtualFile, parsed.relativePath())
      manager.setDependencies(this) {
        file.status = sqldelightValidator.validate(parsed, manager.symbolTable)
        (file.status as Status.ValidationStatus).dependencies.filter { it != virtualFile }
      }
      manager.triggerDependencyRefresh(virtualFile, fromEdit)

      if (file.status is Invalid) return@parseThen

      file.status = SqliteCompiler.write(parsed,
          (file.status as Status.ValidationStatus.Validated).queries,
          parsed.relativePath(),
          virtualFile.getPlatformSpecificPath().moduleDirectory(parsed) + File.separatorChar
      )

      if (file.status is Status.Success) {
        val generatedFile = localFileSystem.findFileByIoFile((file.status as Status.Success).generatedFile)
        if (generatedFile == null) {
          Logger.getInstance(SqlDelightFileViewProvider::class.java)
              .debug("Failed to find the generated file for ${file.virtualFile.path}, " +
                  "it currently is ${file.generatedFile?.virtualFile?.path}")
          return@parseThen
        }
        generatedFile.refresh(true, false)
        if (generatedFile != file.generatedFile?.virtualFile) {
          WriteCommandAction.runWriteCommandAction(file.project, { file.generatedFile?.delete() })
        }
        file.generatedFile = psiManager.findFile(generatedFile)
      }
    }, onError = { parsed, errors ->
      manager.removeFile(virtualFile, fromEdit, SymbolTable(parsed, virtualFile, parsed.relativePath(), errors))
    })
  }

  private fun SqliteParser.ParseContext.relativePath() =
      virtualFile.getPlatformSpecificPath().relativePath(this)

  companion object {
    private val localFileSystem = LocalFileSystem.getInstance()
    private val sqldelightValidator = SqlDelightValidator()
  }
}

internal fun VirtualFile.getPlatformSpecificPath() = path.replace('/', File.separatorChar)
