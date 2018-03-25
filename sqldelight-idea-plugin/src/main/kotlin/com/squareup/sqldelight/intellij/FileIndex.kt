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

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.project.rootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.core.lang.SqlDelightFile

class FileIndex(private val module: Module) : SqlDelightFileIndex {
  private lateinit var contentRoot: VirtualFile
  private val psiManager = PsiManager.getInstance(module.project)
  private val properties by lazy {
    contentRoot = module.rootManager.contentRoots.single()
    if (contentRoot.parent.name == "src") contentRoot = contentRoot.parent.parent
    val file = contentRoot.findChild(SqlDelightPropertiesFile.NAME)
    return@lazy file?.let { SqlDelightPropertiesFile.fromText(it.inputStream.reader().readText()) }
  }

  override val isConfigured: Boolean
    get() = properties != null

  override val packageName by lazy { properties!!.packageName }

  override val outputDirectory by lazy { properties!!.outputDirectory }

  override fun packageName(file: SqlDelightFile): String {
    val original = if (file.parent == null) {
      file.originalFile as SqlDelightFile
    } else {
      file
    }
    val folder = sourceFolders(original)
        .first { PsiTreeUtil.findCommonParent(original, it) != null }
    val folderPath = folder.virtualFile.path
    val filePath = original.virtualFile!!.path
    return filePath.substring(folderPath.length + 1, filePath.indexOf(original.name) - 1).replace('/', '.')
  }

  override fun sourceFolders(file: SqlDelightFile): Collection<PsiDirectory> {
    if (properties == null) return emptyList()
    return properties!!.sourceSets.map { sourceSet ->
      sourceSet.mapNotNull sourceFolder@{ sourceFolder ->
        val vFile = contentRoot.findFileByRelativePath(sourceFolder) ?: return@sourceFolder null
        return@sourceFolder psiManager.findDirectory(vFile)
      }
    }.fold(emptySet()) { currentSources: Collection<PsiDirectory>, sourceSet ->
      if (sourceSet.any { PsiTreeUtil.isAncestor(it, file, false) }) {
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
}
