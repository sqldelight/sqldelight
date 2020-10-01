package com.squareup.sqldelight.core.lang

import com.alecstrong.sql.psi.core.SqlFileBase
import com.intellij.lang.Language
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightProjectService

abstract class SqlDelightFile(
  viewProvider: FileViewProvider,
  language: Language
) : SqlFileBase(viewProvider, language) {
  protected val module: Module?
    get() = SqlDelightProjectService.getInstance(project).module(requireNotNull(virtualFile, { "Null virtualFile" }))

  val generatedDir by lazy {
    val packageName = packageName ?: return@lazy null
    val module = module ?: return@lazy null
    "${SqlDelightFileIndex.getInstance(module).outputDirectory}/${packageName.replace('.', '/')}"
  }

  internal val dialect
    get() = SqlDelightProjectService.getInstance(project).dialectPreset

  internal abstract val packageName: String?

  override fun getVirtualFile(): VirtualFile? {
    if (myOriginalFile != null) return myOriginalFile.virtualFile
    return super.getVirtualFile()
  }

  override fun searchScope(): GlobalSearchScope {
    val default = GlobalSearchScope.fileScope(this)

    val module = module ?: return default
    val index = SqlDelightFileIndex.getInstance(module)
    val sourceFolders = index.sourceFolders(virtualFile ?: return default)
    if (sourceFolders.isEmpty()) return default

    // TODO Deal with database files?

    return sourceFolders
        .map { GlobalSearchScopesCore.directoryScope(project, it, true) }
        .reduce { totalScope, directoryScope -> totalScope.union(directoryScope) }
  }
}
