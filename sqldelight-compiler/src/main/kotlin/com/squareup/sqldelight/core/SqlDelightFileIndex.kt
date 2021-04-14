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

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.squareup.sqldelight.core.lang.SqlDelightFile

interface SqlDelightFileIndex {
  /**
   * @return true if this index is configured to be used by SqlDelight.
   */
  val isConfigured: Boolean

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

  val deriveSchemaFromMigrations: Boolean

  /**
   * @return The package name for a given SqlDelight file. Equal to the relative path under its
   * fixture's sqldelight directory.
   */
  fun packageName(file: SqlDelightFile): String

  /**
   * @return A list of output directory paths generated code should be placed in, relative to
   *   [contentRoot], for the given [file].
   */
  fun outputDirectory(file: SqlDelightFile): List<String>

  /**
   * @return A list of all SQLDelight output directories.
   */
  fun outputDirectories(): List<String>

  /**
   * @return The source roots of sqldelight files for [file].
   */
  fun sourceFolders(file: VirtualFile, includeDependencies: Boolean = true): Collection<VirtualFile>

  /**
   * @return The source roots of sqldelight files for [file].
   */
  fun sourceFolders(file: SqlDelightFile, includeDependencies: Boolean = true): Collection<PsiDirectory>

  companion object {
    fun getInstance(module: Module) = SqlDelightProjectService.getInstance(module.project).fileIndex(module)

    fun sanitizeDirectoryName(name: String): String {
      return name.filter { it.isLetterOrDigit() }
    }
  }
}
