/*
 * Copyright (C) 2016 Square, Inc.
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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileAdapter
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import com.intellij.openapi.vfs.VirtualFilePropertyEvent
import com.intellij.psi.PsiManager
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.intellij.lang.SqlDelightFileViewProvider
import com.squareup.sqldelight.intellij.lang.SqliteFile
import java.util.LinkedHashMap

class SqlDelightVirtualFileListener(val project: Project) : VirtualFileAdapter() {
  // Before the event occurs we need to store a reference to the generated file. After the
  // event has occurred the virtual file will have changed and will have a reference to the
  // new generated file. However, we must perform any virtual file modifications in the
  // fileDeleted/fileMoved functions because they have write access and will be bundled into a
  // single action (which can be undo'd).
  private val generatedFiles = LinkedHashMap<VirtualFile, VirtualFile?>()
  private val VirtualFile.sqliteFile: SqliteFile?
      get() = psiManager.findFile(this) as? SqliteFile
  private val psiManager = PsiManager.getInstance(project)

  private fun storeGeneratedFile(event: VirtualFileEvent) {
    if (project.isDisposed || !event.file.isValid || event.file.extension != SqliteCompiler.FILE_EXTENSION) return
    generatedFiles.put(event.file, event.file.sqliteFile?.generatedVirtualFile)
  }

  private fun removeOldFile(event: VirtualFileEvent) {
    if (project.isDisposed || !event.file.isValid || event.file.extension != SqliteCompiler.FILE_EXTENSION) return
    SqlDelightManager.removeFile(event.file)
    generatedFiles[event.file]?.delete(event.requestor)
    generatedFiles.remove(event.file)
  }

  private fun generateNewFile(event: VirtualFileEvent) {
    if (project.isDisposed || event.file.extension != SqliteCompiler.FILE_EXTENSION) return
    (event.file.sqliteFile?.viewProvider as? SqlDelightFileViewProvider)?.generateJavaInterface(true)
  }

  override fun beforeFileDeletion(event: VirtualFileEvent) {
    storeGeneratedFile(event)
  }

  override fun fileDeleted(event: VirtualFileEvent) {
    removeOldFile(event)
  }

  override fun beforeFileMovement(event: VirtualFileMoveEvent) {
    storeGeneratedFile(event)
  }

  override fun fileMoved(event: VirtualFileMoveEvent) {
    removeOldFile(event)
    generateNewFile(event)
  }

  override fun beforePropertyChange(event: VirtualFilePropertyEvent) {
    if (event.propertyName == VirtualFile.PROP_NAME) {
      storeGeneratedFile(event)
    }
  }

  override fun propertyChanged(event: VirtualFilePropertyEvent) {
    if (event.propertyName == VirtualFile.PROP_NAME) {
      removeOldFile(event)
      generateNewFile(event)
    }
  }
}
