package app.cash.sqldelight.core.lang

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.core.lang.util.AnsiSqlTypeResolver
import app.cash.sqldelight.dialect.api.SqlDelightModule
import app.cash.sqldelight.dialect.api.TypeResolver
import com.alecstrong.sql.psi.core.SqlFileBase
import com.intellij.lang.Language
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import java.util.ServiceLoader

abstract class SqlDelightFile(
  viewProvider: FileViewProvider,
  language: Language,
) : SqlFileBase(viewProvider, language) {
  val module: Module?
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
    get() = SqlDelightProjectService.getInstance(project).dialect

  internal val treatNullAsUnknownForEquality
    get() = SqlDelightProjectService.getInstance(project).treatNullAsUnknownForEquality

  internal val generateAsync
    get() = SqlDelightProjectService.getInstance(project).generateAsync

  internal val typeResolver: TypeResolver by lazy {
    var resolver: TypeResolver = dialect.typeResolver(AnsiSqlTypeResolver)
    ServiceLoader.load(SqlDelightModule::class.java, dialect::class.java.classLoader).forEach {
      resolver = it.typeResolver(resolver)
    }
    resolver
  }

  val packageName: String? by lazy {
    module?.let { module ->
      SqlDelightFileIndex.getInstance(module).packageName(this)
    }
  }

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

  fun findDbFile(): SqlFileBase? {
    val module = module ?: return null

    val manager = PsiManager.getInstance(project)
    var result: SqlFileBase? = null
    val folders = SqlDelightFileIndex.getInstance(module).sourceFolders(virtualFile ?: return null)
    for (dir in folders) {
      VfsUtilCore.iterateChildrenRecursively(
        dir,
        { it.isDirectory || it.fileType == DatabaseFileType },
        { file ->
          if (file.isDirectory) return@iterateChildrenRecursively true

          val vFile = (manager.findViewProvider(file) as? DatabaseFileViewProvider)?.getSchemaFile()
            ?: return@iterateChildrenRecursively true

          val psiFile = manager.findFile(vFile)
          if (psiFile != null && psiFile is SqlFileBase) {
            result = psiFile
            return@iterateChildrenRecursively false
          }

          return@iterateChildrenRecursively true
        },
      )
    }
    return result
  }
}
