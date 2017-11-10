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

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.squareup.sqldelight.core.lang.SqlDelightFile

interface SqlDelightFileIndex {
  /**
   * @return The package name for the whole source set. This is equivalent to the package name
   * found in the manifest file for the current variant.
   */
  val packageName: String

  /**
   * @return The package name for a given SqlDelight file. Equal to the relative path under its
   * fixture's sqldelight directory.
   */
  fun packageName(file: SqlDelightFile): String

  /**
   * @return The source roots of sqldelight files.
   */
  fun sourceFolders(): List<PsiDirectory>

  companion object {
    fun getInstance(project: Project): SqlDelightFileIndex {
      return ServiceManager.getService(project, SqlDelightFileIndex::class.java)
    }
  }
}
