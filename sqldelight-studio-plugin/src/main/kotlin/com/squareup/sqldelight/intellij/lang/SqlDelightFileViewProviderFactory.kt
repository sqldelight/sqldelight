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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.util.containers.MultiMap
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.Status
import com.squareup.sqldelight.Status.ValidationStatus.Invalid
import com.squareup.sqldelight.model.relativePath
import com.squareup.sqldelight.types.SymbolTable
import com.squareup.sqldelight.validation.SqlDelightValidator
import com.squareup.sqldelight.validation.SqlDelightValidator.Companion.ALL_FILE_DEPENDENCY
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
    file.parseThen({ parsed ->
      symbolTable += SymbolTable(parsed, virtualFile)
      synchronized(dependencies) {
        dependencies.entrySet().forEach {
          it.value.remove(this)
        }
        file.status = sqldelightValidator.validate(parsed, symbolTable)
        (file.status as Status.ValidationStatus).dependencies.forEach {
          if (it != virtualFile) {
            dependencies.putValue(it, this)
          }
        }
      }

      triggerDependencyRefresh(virtualFile, fromEdit)

      if (file.status is Invalid) return@parseThen

      file.status = sqliteCompiler.write(
          parsed,
          virtualFile.nameWithoutExtension,
          virtualFile.getPlatformSpecificPath().relativePath(parsed),
          (ModuleUtil.findModuleForPsiElement(file)!!.moduleFile?.parent?.getPlatformSpecificPath() ?: "") + File.separatorChar
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
          file.generatedFile?.delete()
        }
        file.generatedFile = psiManager.findFile(generatedFile)
      }
    }, onError = {
      removeFile(virtualFile, fromEdit)
    })
  }

  companion object {
    private val localFileSystem = LocalFileSystem.getInstance()
    private val sqliteCompiler = SqliteCompiler()
    private val sqldelightValidator = SqlDelightValidator()
    private val dependencies = MultiMap<Any, SqlDelightFileViewProvider>()

    internal var symbolTable = SymbolTable()

    fun removeFile(file: VirtualFile, fromEdit: Boolean = false) {
      symbolTable -= file
      dependencies.entrySet().forEach {
        it.value.removeAll { it.virtualFile == file }
      }
      triggerDependencyRefresh(file, fromEdit)
      dependencies.remove(file)
    }

    private fun triggerDependencyRefresh(file: VirtualFile, fromEdit: Boolean = false) {
      if (fromEdit) {
        (dependencies.get(file) + dependencies.get(ALL_FILE_DEPENDENCY))
            .forEach { it.generateJavaInterface() }
      }
    }
  }
}

internal fun VirtualFile.getPlatformSpecificPath() = path.replace('/', File.separatorChar)
