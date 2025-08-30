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
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.intellij.ProjectService
import app.cash.sqldelight.intellij.util.GeneratedVirtualFile
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.TableElement
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.constrainedReadAndWriteAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import java.io.PrintStream
import java.util.concurrent.CancellationException
import kotlinx.coroutines.launch

class SqlDelightFileViewProviderFactory : FileViewProviderFactory {
  override fun createFileViewProvider(
    file: VirtualFile,
    language: Language,
    manager: PsiManager,
    eventSystemEnabled: Boolean,
  ): FileViewProvider {
    val service = SqlDelightProjectService.getInstance(manager.project) as ProjectService
    val module = service.module(file)
      ?: return SingleRootFileViewProvider(manager, file, eventSystemEnabled)
    return SqlDelightFileViewProvider(manager, file, eventSystemEnabled, language, module, service)
  }
}

private class SqlDelightFileViewProvider(
  manager: PsiManager,
  virtualFile: VirtualFile,
  eventSystemEnabled: Boolean,
  private val language: Language,
  private val module: Module,
  private val service: ProjectService,
) : SingleRootFileViewProvider(manager, virtualFile, eventSystemEnabled, language) {

  private val file: SqlDelightFile
    get() = getPsiInner(language) as SqlDelightFile

  private var filesGenerated = emptyList<VirtualFile>()
    set(value) {
      (field - value.toSet()).forEach { it.delete(this) }
      field = value
    }

  override fun contentsSynchronized() {
    contentsSynchronized(updateTransitive = true)
  }

  private fun contentsSynchronized(updateTransitive: Boolean) {
    super.contentsSynchronized()

    if (!SqlDelightFileIndex.getInstance(module).isConfigured ||
      SqlDelightFileIndex.getInstance(module).sourceFolders(file).isEmpty()
    ) {
      return
    }

    if (ApplicationManager.getApplication().isUnitTestMode) {
      writeFiles(generateSqlDelightCode())
      return
    }

    service.scope.launch {
      constrainedReadAndWriteAction {
        val fileContent = try {
          generateSqlDelightCode()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Throwable) {
          // IDE generating code should be best effort - source of truth is always the gradle
          // build, and its better to ignore the error and try again than crash and require
          // the IDE restarts.
          e.printStackTrace()
          null
        }
        ProgressManager.checkCanceled()
        writeAction {
          writeFiles(fileContent)
        }
      }

      if (!updateTransitive) return@launch

      // Alert other files that rely on tables in this file to synchronize.
      smartReadAction(module.project) {
        file.findChildrenOfType<TableElement>().forEach { queryable ->
          val affectedFiles = ReferencesSearch.search(queryable.tableExposed().tableName)
            .mapNotNull { it.element.containingFile as? SqlDelightFile }
            .distinct()

          affectedFiles.forEach {
            (it.viewProvider as? SqlDelightFileViewProvider)?.contentsSynchronized(false)
          }
        }
      }
    }
  }

  /**
   * Attempt to generate the SQLDelight code for the file represented by the view provider.
   */
  private fun generateSqlDelightCode(): Map<GeneratedFile, StringBuilder> {
    if (module.isDisposed) return emptyMap()

    var shouldGenerate = true
    val annotationHolder = SqlAnnotationHolder { _, _ ->
      shouldGenerate = false
    }

    // File is mutable so create a copy that won't be mutated.
    val file = file.copy() as SqlDelightFile

    shouldGenerate = try {
      PsiTreeUtil.processElements(file) { element ->
        when (element) {
          is PsiErrorElement -> return@processElements false
          is SqlAnnotatedElement -> element.annotate(annotationHolder)
        }
        return@processElements shouldGenerate
      }
    } catch (_: Throwable) {
      // If we encountered an exception while looking for errors, assume it was an error.
      false
    }

    if (shouldGenerate && !ApplicationManager.getApplication().isUnitTestMode) {
      val files = mutableMapOf<GeneratedFile, StringBuilder>()
      val fileAppender = { filePath: String ->
        StringBuilder().also {
          files[GeneratedFile(filePath)] = it
        }
      }
      if (file is SqlDelightQueriesFile) {
        val projectService = SqlDelightProjectService.getInstance(module.project)
        SqlDelightCompiler.writeInterfaces(module, projectService.dialect, file, fileAppender)
      } else if (file is MigrationFile) {
        SqlDelightCompiler.writeInterfaces(file, fileAppender)
      }

      return files
    } else {
      return emptyMap()
    }
  }

  private fun writeFiles(fileContent: Map<GeneratedFile, StringBuilder>?) {
    if (fileContent == null) return

    ApplicationManager.getApplication().runWriteAction {
      val files = mutableListOf<VirtualFile>()
      fileContent.forEach { (file, content) ->
        val vFile: VirtualFile by GeneratedVirtualFile(file.path, module)
        if (!file.transitive) files.add(vFile)
        PrintStream(vFile.getOutputStream(this)).use {
          it.append(content)
        }
      }
      this.filesGenerated = files
    }
  }

  private data class GeneratedFile(
    val path: String,
    val transitive: Boolean = false,
  )
}
