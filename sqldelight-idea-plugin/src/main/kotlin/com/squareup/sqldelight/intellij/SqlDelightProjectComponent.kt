package com.squareup.sqldelight.intellij

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import org.jetbrains.kotlin.idea.util.projectStructure.getModule

class SqlDelightProjectComponent(
  private val project: Project
) : ProjectComponent, VirtualFileListener {
  override fun projectOpened() {
    project.baseDir.findFileByRelativePath(".idea/sqldelight")?.let { propertiesRoot ->
      forPropertiesFilesUnder(propertiesRoot) {
        initializeIndexForPropertiesFile(it)
      }
    }
    VirtualFileManager.getInstance().addVirtualFileListener(this)
  }

  override fun projectClosed() {
    VirtualFileManager.getInstance().removeVirtualFileListener(this)
  }

  override fun contentsChanged(event: VirtualFileEvent) {
    forPropertiesFilesUnder(event.file) {
      initializeIndexForPropertiesFile(it)
    }
  }

  override fun beforeFileDeletion(event: VirtualFileEvent) {
    forPropertiesFilesUnder(event.file) {
      moduleForPropertiesFile(it)?.let { module ->
        SqlDelightFileIndex.removeModule(module)
      }
    }
  }

  override fun fileCreated(event: VirtualFileEvent) {
    forPropertiesFilesUnder(event.file) {
      initializeIndexForPropertiesFile(it)
    }
  }

  private fun forPropertiesFilesUnder(file: VirtualFile, action: (VirtualFile) -> Unit) {
    VfsUtilCore.iterateChildrenRecursively(file, VirtualFileFilter { true }, ContentIterator {
      if (it.name != SqlDelightPropertiesFile.NAME) return@ContentIterator true
      action(it)
      return@ContentIterator true
    })
  }

  private fun initializeIndexForPropertiesFile(file: VirtualFile) {
    val module = moduleForPropertiesFile(file) ?: return
    val contentRoot = contentRootForPropertiesFile(file) ?: return
    val propertiesFile = SqlDelightPropertiesFile.fromText(file.inputStream.reader().readText())!!

    SqlDelightFileIndex.setInstance(module, FileIndex(propertiesFile, contentRoot))
  }

  private fun contentRootForPropertiesFile(propertiesFile: VirtualFile): VirtualFile? {
    val propertiesFolder = project.propertiesFolder ?: return null
    var parent = propertiesFile.parent
    var path = ""
    while (parent != propertiesFolder) {
      path = "/${parent.name}$path"
      parent = parent.parent ?: return null
    }
    return project.baseDir.findFileByRelativePath(path)
  }

  private fun moduleForPropertiesFile(propertiesFile: VirtualFile): Module? {
    return contentRootForPropertiesFile(propertiesFile)?.getModule(project)
  }
}

internal val Project.propertiesFolder: VirtualFile?
  get() = baseDir.findFileByRelativePath(".idea/sqldelight")