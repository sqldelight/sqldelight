package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.core.SqlDelightSourceFolder
import com.intellij.ide.actions.CreateDirectoryCompletionContributor
import com.intellij.ide.actions.CreateDirectoryCompletionContributor.Variant
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiDirectory
import org.jetbrains.jps.model.module.UnknownSourceRootType
import org.jetbrains.kotlin.konan.file.File

internal class SqlDelightDirectoryCompletionContributor : CreateDirectoryCompletionContributor {
  override fun getDescription(): String {
    return "SqlDelight"
  }

  override fun getVariants(directory: PsiDirectory): Collection<Variant> {
    val module = ModuleUtil.findModuleForFile(directory.virtualFile, directory.project)
      ?: return emptyList()
    val projectService = SqlDelightProjectService.getInstance(directory.project)
    val fileIndex = projectService.fileIndex(module)
    if (!fileIndex.isConfigured) {
      return emptyList()
    }

    val packageName = fileIndex.packageName.replace(".", File.separator)

    fun packagePath(sourceFolderPath: String): String {
      return if (packageName.isEmpty()) {
        sourceFolderPath
      } else {
        "$sourceFolderPath${File.separator}$packageName"
      }
    }

    fun migrationsPath(sourceFolderPath: String): String {
      return "$sourceFolderPath${File.separator}migrations"
    }

    val sourceRootType = UnknownSourceRootType.getInstance("sqldelight")

    return fileIndex.sourceFolders(true)
      .flatten()
      .fold(mutableListOf()) { acc: MutableList<Variant>, sourceFolders: SqlDelightSourceFolder ->
        val sourceFolderPath = sourceFolders.folder.path
        acc.apply {
          add(Variant(packagePath(sourceFolderPath), sourceRootType))
          add(Variant(migrationsPath(sourceFolderPath), sourceRootType))
        }
      }
  }
}
