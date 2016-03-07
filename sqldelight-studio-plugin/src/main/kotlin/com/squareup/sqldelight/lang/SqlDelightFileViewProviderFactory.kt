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
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.Status
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
    documentManager.performWhenAllCommitted { generateJavaInterface() }
  }

  internal fun generateJavaInterface() {
    file.parseThen { parsed ->
      symbolTable += SymbolTable(parsed, virtualFile)
      file.status = sqdelightValidator.validate(parsed, symbolTable)
      if (file.status is Status.Invalid) return@parseThen

      file.status = sqliteCompiler.write(
          parsed,
          file.virtualFile.nameWithoutExtension,
          file.virtualFile.getPlatformSpecificPath().relativePath(parsed),
          ModuleUtil.findModuleForPsiElement(file)!!.moduleFile!!.parent.getPlatformSpecificPath() + File.separatorChar,
          symbolTable
      )

      if (file.status is Status.Success) {
        val generatedFile = localFileSystem.findFileByIoFile((file.status as Status.Success).generatedFile)
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

    internal var symbolTable = SymbolTable()
  }
}

internal fun VirtualFile.getPlatformSpecificPath() = path.replace('/', File.separatorChar)
