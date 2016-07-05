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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.squareup.javapoet.JavaFile
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.SqlitePluginException
import com.squareup.sqldelight.Status
import com.squareup.sqldelight.Status.ValidationStatus
import com.squareup.sqldelight.intellij.SqlDelightManager
import com.squareup.sqldelight.intellij.util.moduleDirectory
import com.squareup.sqldelight.model.pathPackage
import com.squareup.sqldelight.types.SymbolTable
import com.squareup.sqldelight.validation.SqlDelightValidator

class SqlDelightFileViewProviderFactory : FileViewProviderFactory {
  override fun createFileViewProvider(virtualFile: VirtualFile, language: Language,
      psiManager: PsiManager, eventSystemEnabled: Boolean): FileViewProvider {
    if (virtualFile.moduleDirectory() == null) {
      // .sq file is not under src/variant/sqldelight.
      return SingleRootFileViewProvider(psiManager, virtualFile, eventSystemEnabled)
    }
    return SqlDelightFileViewProvider(virtualFile, language, psiManager, eventSystemEnabled)
  }
}

internal class SqlDelightFileViewProvider(
    virtualFile: VirtualFile,
    language: Language,
    psiManager: PsiManager,
    eventSystemEnabled: Boolean
) : SingleRootFileViewProvider(psiManager, virtualFile, eventSystemEnabled, language) {

  val file: SqliteFile by lazy {
    getPsiInner(SqliteLanguage.INSTANCE) as SqliteFile
  }

  override fun contentsSynchronized() {
    super.contentsSynchronized()
    generateJavaInterface(true)
  }

  internal fun generateJavaInterface(fromEdit: Boolean = false) {
    // Mark the file as dirty and re-parse.
    file.dirty = true
    file.parseThen({ parsed ->
      var status: Status = validate(parsed, fromEdit) ?: return@parseThen
      if (status is ValidationStatus.Validated) {
        status = parsed.compile(status)
        if (status is Status.Success) {
          file.write(JavaFile.builder(file.relativePath.pathPackage(), status.model).build().toString())
        }
      }
      file.status = status
    }, onError = { parsed, errors ->
      val manager = SqlDelightManager.getInstance(file) ?: return@parseThen
      if (parsed.sql_stmt_list() == null) {
        manager.removeFile(virtualFile, fromEdit)
        throw SqlitePluginException(parsed, parsed.exception.message ?: parsed.error()?.text ?: "error")
      }
      manager.removeFile(virtualFile, fromEdit, SymbolTable(parsed, virtualFile, file.relativePath, errors))
    })
  }

  private fun SqliteParser.ParseContext.compile(validationStatus: ValidationStatus.Validated) =
      SqliteCompiler.compile(this, validationStatus.queries, file.relativePath)

  private fun validate(parsed: SqliteParser.ParseContext, fromEdit: Boolean): ValidationStatus? {
    val manager = SqlDelightManager.getInstance(file) ?: return null
    manager.symbolTable += SymbolTable(parsed, virtualFile, file.relativePath)

    val status = sqldelightValidator.validate(file.relativePath, parsed, manager.symbolTable)
    manager.setDependencies(this) {
      status.dependencies.filter { it != virtualFile }
    }
    manager.triggerDependencyRefresh(virtualFile, fromEdit)
    return status
  }

  companion object {
    private val sqldelightValidator = SqlDelightValidator()
  }
}
