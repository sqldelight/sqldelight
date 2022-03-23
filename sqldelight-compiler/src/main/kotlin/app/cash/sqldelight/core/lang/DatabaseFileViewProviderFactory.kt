package app.cash.sqldelight.core.lang

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.SqlDelightProjectService
import com.alecstrong.sql.psi.core.DialectPreset
import com.intellij.lang.Language
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.testFramework.LightVirtualFile
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseFileViewProviderFactory : FileViewProviderFactory {
  override fun createFileViewProvider(
    vFile: VirtualFile,
    language: Language?,
    psiManager: PsiManager,
    eventSystemEnabled: Boolean
  ): FileViewProvider {
    fun default() = SingleRootFileViewProvider(psiManager, vFile, eventSystemEnabled)

    if (vFile.fileType != DatabaseFileType) return default()

    val projectService = SqlDelightProjectService.getInstance(psiManager.project)
    if (!projectService.isSupportedSqlite()) return default()

    val module = projectService.module(vFile) ?: return default()
    val index = SqlDelightFileIndex.getInstance(module)
    if (index.deriveSchemaFromMigrations) return default()

    // If this database is unversioned, continue.
    val version = vFile.version ?: return default()

    val lowestVersion = ProjectFileIndex.getInstance(psiManager.project).iterateContent {
      if (it.fileType != DatabaseFileType && it.fileType != MigrationFileType) return@iterateContent true
      if (module != projectService.module(vFile)) return@iterateContent true

      val otherVersion = it.version
      if (otherVersion != null && otherVersion < version) {
        return@iterateContent false
      }
      return@iterateContent true
    }

    // Only parse a db if it is the base.
    if (!lowestVersion) return default()

    return DatabaseFileViewProvider(psiManager, vFile, eventSystemEnabled)
  }
}

internal class DatabaseFileViewProvider(
  manager: PsiManager,
  virtualFile: VirtualFile,
  eventSystemEnabled: Boolean
) : SingleRootFileViewProvider(manager, virtualFile, eventSystemEnabled, DatabaseFileType) {
  private var schemaFile: VirtualFile? = null

  fun getSchemaFile(): VirtualFile? {
    schemaFile?.let { return it }

    val virtualFile = super.getVirtualFile()
    try {
      val statements = createConnection(virtualFile.path).use {
        it.prepareStatement("SELECT sql FROM sqlite_master WHERE sql IS NOT NULL;").use {
          it.executeQuery().use {
            mutableListOf<String>().apply {
              while (it.next()) {
                add("${it.getString(1)};")
              }
            }
          }
        }
      }
      return LightVirtualFile(
        "${virtualFile.version!! - 1}.db",
        MigrationFileType,
        statements.joinToString(separator = "\n")
      ).also { schemaFile = it }
    } catch (e: Exception) {
      return null
    }
  }

  private fun createConnection(path: String): Connection {
    return try {
      DriverManager.getConnection("jdbc:sqlite:$path")
    } catch (e: SQLException) {
      DriverManager.getConnection("jdbc:sqlite:$path")
    }
  }
}

private val VirtualFile.version
  get() = nameWithoutExtension.filter { it in '0'..'9' }.toIntOrNull()

private fun SqlDelightProjectService.isSupportedSqlite(): Boolean = when (dialectPreset) {
  DialectPreset.SQLITE_3_18, DialectPreset.SQLITE_3_24, DialectPreset.SQLITE_3_25, DialectPreset.SQLITE_3_35 -> true
  DialectPreset.MYSQL, DialectPreset.POSTGRESQL, DialectPreset.HSQL -> false
}
