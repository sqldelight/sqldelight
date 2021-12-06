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

package app.cash.sqldelight.intellij.lang

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.core.compiler.SqlDelightCompiler
import app.cash.sqldelight.core.lang.MigrationFile
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.intellij.util.GeneratedVirtualFile
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.util.PsiTreeUtil
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
    return SqlDelightFileViewProvider(manager, file, eventSystemEnabled, language)
  }
}

private class SqlDelightFileViewProvider(
  manager: PsiManager,
  virtualFile: VirtualFile,
  eventSystemEnabled: Boolean,
  private val language: Language
) : SingleRootFileViewProvider(manager, virtualFile, eventSystemEnabled, language) {

  private val file: SqlDelightFile
    get() = getPsiInner(language) as SqlDelightFile

  override fun contentsSynchronized() {
    super.contentsSynchronized()

    FileGeneratorService.getInstance(file.project).generateFiles(file)
  }
}
