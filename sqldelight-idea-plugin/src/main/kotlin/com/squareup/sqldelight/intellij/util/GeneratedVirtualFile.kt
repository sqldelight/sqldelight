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

package com.squareup.sqldelight.intellij.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import kotlin.reflect.KProperty

internal class GeneratedVirtualFile(private val path: String) {
  private val applicationManager = ApplicationManager.getApplication()
  private val localFileSystem = LocalFileSystem.getInstance()
  private var backingFile: VirtualFile? = null

  operator fun getValue(
    thisRef: Nothing?,
    property: KProperty<*>
  ): VirtualFile {
    applicationManager.assertWriteAccessAllowed()
    synchronized(this) {
      val backingFile = this.backingFile
      if (backingFile == null || !backingFile.exists()) {
        var file = localFileSystem.findFileByPath(path)
        if (file == null || file.exists()) {
          file = getOrCreateFile(path)
        }
        if (!file.exists()) {
          throw IllegalStateException("VirtualFile $path still doesn't exist after creating.")
        }
        this.backingFile = file
        return file
      } else {
        return backingFile
      }
    }
  }

  private fun getOrCreateFile(path: String): VirtualFile {
    val indexOfName = path.lastIndexOf('/')
    val parent = getOrCreateDirectory(path.substring(0, indexOfName))
    return parent.findOrCreateChildData(this, path.substring(indexOfName + 1, path.length))
  }

  private fun getOrCreateDirectory(path: String): VirtualFile {
    val indexOfName = path.lastIndexOf('/')
    val parentPath = path.substring(0, indexOfName)
    var parent = localFileSystem.findFileByPath(parentPath)
    if (parent == null || !parent.exists()) {
      parent = getOrCreateDirectory(parentPath)
      if (!parent.exists() || !parent.isDirectory) throw AssertionError()
    }

    val name = path.substring(indexOfName + 1, path.length)
    return parent.findChild(name) ?: parent.createChildDirectory(this, name)
  }
}