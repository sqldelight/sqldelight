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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.intellij.util.isAncestorOf
import org.jetbrains.kotlin.idea.refactoring.toPsiDirectory

class FileIndex(
  private val properties: SqlDelightPropertiesFile,
  override val contentRoot: VirtualFile
) : SqlDelightFileIndex {
  override val isConfigured = true
  override val packageName = properties.packageName
  override val outputDirectory = properties.outputDirectory
  override val className = properties.className

  override fun packageName(file: SqlDelightFile): String {
    val original = if (file.parent == null) {
      file.originalFile as SqlDelightFile
    } else {
      file
    }
    val folder = sourceFolders(original)
        .firstOrNull { PsiTreeUtil.findCommonParent(original, it) != null } ?: return ""
    val folderPath = folder.virtualFile.path
    val filePath = original.virtualFile!!.path
    return filePath.substring(folderPath.length + 1, filePath.indexOf(original.name) - 1).replace('/', '.')
  }

  override fun sourceFolders(file: VirtualFile): Collection<VirtualFile> {
    return properties.sourceSets.map { sourceSet ->
      sourceSet.mapNotNull { contentRoot.findFileByRelativePath(it) }
    }.fold(emptySet()) { currentSources: Collection<VirtualFile>, sourceSet ->
      if (sourceSet.any { it.isAncestorOf(file) }) {
        // File is in this source set.
        if (currentSources.isEmpty()) {
          return@fold sourceSet
        } else {
          // File also in another source set! The files available sources is the intersection.
          return@fold currentSources.intersect(sourceSet)
        }
      }
      return@fold currentSources
    }
  }

  override fun sourceFolders(file: SqlDelightFile): Collection<PsiDirectory> {
    return sourceFolders(file.virtualFile!!).map { it.toPsiDirectory(file.project)!! }
  }
}
