/*
 * Copyright (C) 2018 Square, Inc.
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

import com.alecstrong.sqlite.psi.core.SqliteAnnotationHolder
import com.alecstrong.sqlite.psi.core.psi.SqliteAnnotatedElement
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightProjectService
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.SqlDelightLanguage
import com.squareup.sqldelight.intellij.util.GeneratedVirtualFile
import java.io.PrintStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SqlDelightFileViewProviderFactory : FileViewProviderFactory {
  override fun createFileViewProvider(
    file: VirtualFile,
    language: Language,
    manager: PsiManager,
    eventSystemEnabled: Boolean
  ): FileViewProvider {
    val module = SqlDelightProjectService.getInstance(manager.project).module(file)
    if (module == null || !SqlDelightFileIndex.getInstance(module).isConfigured) {
      return SingleRootFileViewProvider(manager, file, eventSystemEnabled)
    }
    return SqlDelightFileViewProvider(manager, file, eventSystemEnabled, language, module)
  }
}

private class SqlDelightFileViewProvider(
  manager: PsiManager,
  virtualFile: VirtualFile,
  eventSystemEnabled: Boolean,
  language: Language,
  private val module: Module
) : SingleRootFileViewProvider(manager, virtualFile, eventSystemEnabled, language) {
  private val threadPool = Executors.newScheduledThreadPool(1)

  private val file: SqlDelightFile
    get() = getPsiInner(SqlDelightLanguage) as SqlDelightFile

  private var filesGenerated = emptyList<String>()
    set(value) {
      (field - value).forEach { filePath ->
        val vFile: VirtualFile by GeneratedVirtualFile(filePath)
        vFile.delete(this)
      }
      field = value
    }


  private var condition = WriteCondition()

  override fun contentsSynchronized() {
    super.contentsSynchronized()

    condition.invalidated.set(true)

    val thisCondition = WriteCondition()
    condition = thisCondition
    threadPool.schedule({
      ApplicationManager.getApplication().invokeLater(
          Runnable { generateSqlDelightCode() },
          thisCondition
      )
    }, 1, TimeUnit.SECONDS)
  }

  /**
   * Attempt to generate the SQLDelight code for the file represented by the view provider.
   */
  private fun generateSqlDelightCode() {
    var shouldGenerate = true
    val annotationHolder = object : SqliteAnnotationHolder {
      override fun createErrorAnnotation(element: PsiElement, s: String) {
        shouldGenerate = false
      }
    }

    // File is mutable so create a copy that wont be mutated.
    val file = file.copyWithSymbols() as SqlDelightFile

    shouldGenerate = PsiTreeUtil.processElements(file) { element ->
      when (element) {
        is PsiErrorElement -> return@processElements false
        is SqliteAnnotatedElement -> element.annotate(annotationHolder)
      }
      return@processElements shouldGenerate
    }

    if (shouldGenerate) ApplicationManager.getApplication().runWriteAction {
      val files = mutableListOf<String>()
      SqlDelightCompiler.compile(module, file) { filePath ->
        files.add(filePath)
        val vFile: VirtualFile by GeneratedVirtualFile(filePath)
        PrintStream(vFile.getOutputStream(this))
      }
      this.filesGenerated = files
    }
  }

  private class WriteCondition : Condition<Any> {
    var invalidated = AtomicBoolean(false)

    override fun value(t: Any?) = invalidated.get()
  }
}