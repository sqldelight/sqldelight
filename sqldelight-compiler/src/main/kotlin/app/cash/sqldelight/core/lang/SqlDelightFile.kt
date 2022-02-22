package app.cash.sqldelight.core.lang

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.SqlDelightProjectService
import com.alecstrong.sql.psi.core.SqlFileBase
import com.intellij.lang.Language
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore

abstract class SqlDelightFile(
  viewProvider: FileViewProvider,
  language: Language
) : SqlFileBase(viewProvider, language) {
  protected val module: Module?
    get() = virtualFile?.let { SqlDelightProjectService.getInstance(project).module(it) }

  val generatedDirectories by lazy {
    val packageName = packageName ?: return@lazy null
    generatedDirectories(packageName)
  }

  internal fun generatedDirectories(packageName: String): List<String>? {
    val module = module ?: return null
    return SqlDelightFileIndex.getInstance(module).outputDirectory(this).map { outputDirectory ->
      "$outputDirectory/${packageName.replace('.', '/')}"
    }
  }

  internal val dialect
    get() = SqlDelightProjectService.getInstance(project).dialectPreset

  abstract val packageName: String?

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
