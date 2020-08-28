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
package com.squareup.sqldelight.intellij

import com.alecstrong.sql.psi.core.DialectPreset
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.impl.PsiDocumentManagerImpl
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightProjectService
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import java.io.File
import org.gradle.tooling.GradleConnector

class ProjectService(val project: Project) : SqlDelightProjectService, Disposable {
  private val connector: GradleConnector by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    GradleConnector.newConnector()
  }

  private val fileIndexes = LinkedHashMap<Module, SqlDelightFileIndex>()

  override fun dispose() {
    connector.disconnect()
  }

  override var dialectPreset: DialectPreset = DialectPreset.SQLITE_3_18
    set(value) {
      val invalidate = field != value
      field = value
      if (invalidate) {
        val files = mutableListOf<VirtualFile>()
        ProjectRootManager.getInstance(project).fileIndex.iterateContent { vFile ->
          if (vFile.fileType != SqlDelightFileType) {
            return@iterateContent true
          }
          files += vFile
          return@iterateContent true
        }
        ApplicationManager.getApplication().invokeLater {
          (PsiDocumentManager.getInstance(project) as PsiDocumentManagerImpl)
              .reparseFiles(files, true)
        }
      }
    }

  override fun module(vFile: VirtualFile): Module? {
    return ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(vFile)
  }

  override fun fileIndex(module: Module): SqlDelightFileIndex {
    val index = fileIndexes[module]
    if (index != null) return index

    val projectConnection = connector
        .forProjectDirectory(File(ExternalSystemApiUtil.getExternalProjectPath(module) ?: return defaultIndex))
        .connect()

    val properties = try {
      projectConnection.getModel(SqlDelightPropertiesFile::class.java)!!
    } catch (e: Throwable) {
      return defaultIndex.also { fileIndexes[module] = it }
    } finally {
      projectConnection.close()
    }

    dialectPreset = properties.databases.first().dialectPreset
    return FileIndex(properties.databases.first()).also {
      Disposer.register(module, Disposable { fileIndexes.remove(module) })
      fileIndexes[module] = it
    }
  }

  companion object {
    internal var defaultIndex: SqlDelightFileIndex = SqlDelightFileIndexImpl()
  }
}
