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
import app.cash.sqldelight.core.lang.MigrationFileType
import app.cash.sqldelight.core.lang.MigrationParserDefinition
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.intellij.gradle.FileIndexMap
import app.cash.sqldelight.intellij.run.window.SqlDelightToolWindowFactory
import app.cash.sqldelight.intellij.util.GeneratedVirtualFile
import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.SqlParserUtil
import com.alecstrong.sql.psi.core.psi.InvalidElementDetectedException
import com.intellij.icons.AllIcons
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.projectView.ProjectView
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
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiDocumentManagerImpl
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.jvm.jvmName
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import timber.log.Timber

class ProjectService(val project: Project) :
  SqlDelightProjectService,
  Disposable {
  private var fileIndexes: FileIndexMap?
  private val loggingTree = LoggerTree(Logger.getInstance("SQLDelight[${project.name}]"))

  init {
    Timber.plant(loggingTree)

    val path = Path.of(project.basePath!!)
    fileIndexes = if (ProjectUtil.isValidProjectPath(path)) {
      FileIndexMap()
    } else {
      // A gradle sync is needed before the file index map initializes.
      null
    }

    if (!ApplicationManager.getApplication().isUnitTestMode) {
      val currentProjectViewPane = ProjectView.getInstance(project).currentProjectViewPane
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
              events.mapNotNull { it.file }
                .filter { it.findConfiguredFileIndex() != null }
                .forEach { virtualFile ->
                  val index = requireNotNull(virtualFile.findConfiguredFileIndex())
                  val filePath = try {
                    virtualFile.toNioPath().toAbsolutePath()
                  } catch (exception: UnsupportedOperationException) {
                    Paths.get("").toAbsolutePath()
                  }
                  val sourceFolder = index.sourceFolders(true)
                    .flatten()
                    .firstOrNull { sourceFolder ->
                      filePath.startsWith(sourceFolder.folder.toPath())
                    }
                  if (sourceFolder != null) {
                    currentProjectViewPane?.updateFromRoot(true)
                  }
                }
            }
          },
        )
    }
  }

  private fun VirtualFile.findConfiguredFileIndex(): SqlDelightFileIndex? {
    val module = getModule(project) ?: return null
    val fileIndex = SqlDelightFileIndex.getInstance(module)
    return fileIndex.takeIf { it.isConfigured }
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
          try {
            SqlDelightCompiler.writeDatabaseInterface(module, file, module.name, fileAppender)
          } catch (e: InvalidElementDetectedException) {
            // Since this is an IDE step, it's possible it happens during some element invalidation.
            // In those cases its okay to ignore this time and wait until the next attempt at
            // codegen.
          }
        }
      }
    }
  }

  override var treatNullAsUnknownForEquality: Boolean = false

  override var generateAsync: Boolean = false

  override var dialect: SqlDelightDialect = MissingDialect()

  override fun setDialect(dialect: SqlDelightDialect, shouldInvalidate: Boolean) {
    if (shouldInvalidate || dialect::class.jvmName != this.dialect::class.jvmName) {
      Timber.i("Setting dialect from ${this.dialect} to $dialect")
      this.dialect = dialect
      MigrationParserDefinition.stubVersion++
      ApplicationManager.getApplication().runReadAction { invalidateAllFiles() }
      ApplicationManager.getApplication().invokeLater {
        ToolWindowManager.getInstance(project).getToolWindow("SqlDelight")?.remove()

        val connectionManager = dialect.connectionManager
        if (connectionManager != null) {
          ToolWindowManager.getInstance(project).registerToolWindow(id = "SqlDelight") {
            anchor = ToolWindowAnchor.BOTTOM
            contentFactory = SqlDelightToolWindowFactory(connectionManager)
            canCloseContent = true
            icon = dialect.icon
          }.apply {
            show()
            hide()
          }
        }
      }
    }
  }

  private fun invalidateAllFiles() {
    val files = mutableListOf<VirtualFile>()
    ProjectRootManager.getInstance(project).fileIndex.iterateContent { vFile ->
      if (vFile.fileType != SqlDelightFileType && vFile.fileType != MigrationFileType) {
        return@iterateContent true
      }
      files += vFile
      (PsiManager.getInstance(project).findFile(vFile) as SqlFileBase?)?.apply {
        setTreeElementPointer(null)
        subtreeChanged()
      }
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

  override fun module(vFile: VirtualFile): Module? {
    return ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(vFile)
  }

  override fun fileIndex(module: Module): SqlDelightFileIndex {
    return fileIndexes?.get(module) ?: FileIndexMap.defaultIndex
  }

  private class MissingDialect : SqlDelightDialect {
    override val icon = AllIcons.Providers.Sqlite
    override fun setup() {
      SqlParserUtil.reset()
    }
    override fun typeResolver(parentResolver: TypeResolver) = parentResolver
  }
}
