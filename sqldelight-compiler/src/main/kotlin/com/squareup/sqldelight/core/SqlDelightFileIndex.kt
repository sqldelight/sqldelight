/*
 * Copyright (C) 2017 Square, Inc.
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
package com.squareup.sqldelight.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiManagerEx
import com.squareup.sqldelight.core.lang.MigrationFile
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.SqlDelightFileType

interface SqlDelightFileIndex {
  /**
   * @return true if this index is configured to be used by SqlDelight.
   */
  val isConfigured: Boolean

  /**
   * @return the path to the output directory generated code should be placed in, relative to
   *   [contentRoot]
   */
  val outputDirectory: String

  /**
   * @return The package name for the whole source set. This is equivalent to the package name
   * found in the manifest file for the current variant.
   */
  val packageName: String

  /**
   * @return The package name for the generated type which holds all the query files.
   */
  val className: String

  /**
   * @return The list of fully qualified classnames of databases this module depends on.
   */
  val dependencies: List<SqlDelightDatabaseName>

  /**
   * @return The content root for the [Module] backing this index.
   */
  val contentRoot: VirtualFile

  /**
   * @return The integer ordering this migration file will be run in.
   */
  fun ordering(file: MigrationFile): Int?

  /**
   * @return The package name for a given SqlDelight file. Equal to the relative path under its
   * fixture's sqldelight directory.
   */
  fun packageName(file: SqlDelightFile): String

  /**
   * @return The source roots of sqldelight files for [file].
   */
  fun sourceFolders(file: VirtualFile, includeDependencies: Boolean = true): Collection<VirtualFile>

  /**
   * @return The source roots of sqldelight files for [file].
   */
  fun sourceFolders(file: SqlDelightFile, includeDependencies: Boolean = true): Collection<PsiDirectory>

  companion object {
    private val DEFAULT = SqlDelightFileIndexImpl()

    private val indexes = LinkedHashMap<Module, SqlDelightFileIndex>()

    fun getInstance(module: Module): SqlDelightFileIndex {
      module.project.baseDir?.findChild(".idea")?.refresh(
          /* asynchronous = */ ApplicationManager.getApplication().isReadAccessAllowed,
          /* recursive = */ true
      )
      return indexes.getOrDefault(module, DEFAULT)
    }

    fun setInstance(module: Module, index: SqlDelightFileIndex) {
      indexes[module] = index
    }

    fun removeModule(module: Module) {
      val root = indexes[module]?.contentRoot
      indexes.remove(module)
      if (root != null) {
        val fileManager = (PsiManager.getInstance(module.project) as PsiManagerEx).fileManager
        VfsUtilCore.iterateChildrenRecursively(root, VirtualFileFilter { true }, ContentIterator {
          if (it.fileType == SqlDelightFileType) {
            fileManager.setViewProvider(it, null)
          }
          return@ContentIterator true
        })
      }
    }

    fun sanitizeDirectoryName(name: String): String {
      return name.filter(Char::isLetter)
    }
  }
}
