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
package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.core.compiler.SqlDelightCompiler
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.intellij.gradle.FileIndexMap
import app.cash.sqldelight.intellij.util.GeneratedVirtualFile
import com.alecstrong.sql.psi.core.SqlParserUtil
import com.intellij.icons.AllIcons
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiDocumentManagerImpl
import com.squareup.kotlinpoet.ClassName
import timber.log.Timber
import java.io.PrintStream
import java.nio.file.Path

class ProjectService(val project: Project) : SqlDelightProjectService, Disposable {
  private var fileIndexes: FileIndexMap?
  private val loggingTree = LoggerTree(Logger.getInstance("SQLDelight[${project.name}]"))

  init {
    Timber.plant(loggingTree)

    val path = Path.of(project.basePath!!)
    if (ProjectUtil.isValidProjectPath(path)) {
      fileIndexes = FileIndexMap()
    } else {
      // A gradle sync is needed before the file index map initializes.
      fileIndexes = null
    }

    if (!ApplicationManager.getApplication().isUnitTestMode) {
      project.messageBus.connect()
        .subscribe(
          VirtualFileManager.VFS_CHANGES,
          object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
              events.filter { it.file?.fileType == SqlDelightFileType }.forEach { event ->
                if (event is VFileCreateEvent || event is VFileMoveEvent) {
                  PsiManager.getInstance(project).findViewProvider(event.file!!)
                    ?.contentsSynchronized()
                }
                if (event is VFileCreateEvent || event is VFileDeleteEvent || event is VFileMoveEvent) {
                  event.file?.let { generateDatabaseOnSync(it) }
                }
              }
            }
          }
        )
    }
  }

  override fun dispose() {
    Timber.uproot(loggingTree)
  }

  override fun resetIndex() {
    fileIndexes = FileIndexMap()
  }

  override fun clearIndex() {
    fileIndexes?.close()
    fileIndexes = null
  }

  private fun generateDatabaseOnSync(vFile: VirtualFile) {
    val module = module(vFile) ?: return
    if (fileIndex(module) !is FileIndex) return

    val file = PsiManager.getInstance(project).findFile(vFile) as SqlDelightFile? ?: return

    val fileAppender = { filePath: String ->
      val vFile: VirtualFile by GeneratedVirtualFile(filePath, module)
      PrintStream(vFile.getOutputStream(this))
    }

    with(ApplicationManager.getApplication()) {
      invokeLater {
        runWriteAction {
          SqlDelightCompiler.writeDatabaseInterface(module, file, module.name, fileAppender)
        }
      }
    }
  }

  private var _dialect: SqlDelightDialect? = null
  override var dialect: SqlDelightDialect
    get() = _dialect ?: MissingDialect()
    set(value) {
      val invalidate = _dialect == null || _dialect!!::class.java.name != value::class.java.name
      _dialect = value
      if (invalidate) {
        Timber.i("Setting dialect from $_dialect to $value")
        val files = mutableListOf<VirtualFile>()
        ProjectRootManager.getInstance(project).fileIndex.iterateContent { vFile ->
          if (vFile.fileType != SqlDelightFileType) {
            return@iterateContent true
          }
          files += vFile
          return@iterateContent true
        }
        Timber.i("Invalidating ${files.size} files")
        ApplicationManager.getApplication().invokeLater {
          if (project.isDisposed) return@invokeLater
          Timber.i("Reparsing ${files.size} files")
          (PsiDocumentManager.getInstance(project) as PsiDocumentManagerImpl)
            .reparseFiles(files, true)
        }
      }
    }

  override fun module(vFile: VirtualFile): Module? {
    return ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(vFile)
  }

  override fun fileIndex(module: Module): SqlDelightFileIndex {
    return fileIndexes?.get(module) ?: FileIndexMap.defaultIndex
  }

  private class MissingDialect : SqlDelightDialect {
    override val driverType: ClassName get() = ClassName("app.cash.sqldelight.db", "SqlDriver")
    override val cursorType: ClassName get() = ClassName("app.cash.sqldelight.db", "SqlCursor")
    override val preparedStatementType = ClassName("app.cash.sqldelight.db", "SqlPreparedStatement")
    override val icon = AllIcons.Providers.Sqlite
    override fun setup() { SqlParserUtil.reset() }
    override fun typeResolver(parentResolver: TypeResolver) = parentResolver
  }
}
