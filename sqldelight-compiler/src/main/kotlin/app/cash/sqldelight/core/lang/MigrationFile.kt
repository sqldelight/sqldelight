package app.cash.sqldelight.core.lang

import app.cash.sqldelight.core.SqlDelightFileIndex
import com.alecstrong.sql.psi.core.SqlFileBase
import com.intellij.psi.FileViewProvider

class MigrationFile(
  viewProvider: FileViewProvider
) : SqlDelightFile(viewProvider, MigrationLanguage) {
  val version: Int by lazy {
    name.substringBeforeLast(".${MigrationFileType.EXTENSION}")
      .filter { it in '0'..'9' }.toIntOrNull() ?: 0
  }

  internal fun sqliteStatements() = sqlStmtList!!.stmtList

  override val packageName
    get() = module?.let { module -> SqlDelightFileIndex.getInstance(module).packageName(this) }

  override val order
    get() = version

  override fun getFileType() = MigrationFileType

  override fun baseContributorFile(): SqlFileBase? {
    val module = module
    if (module == null || SqlDelightFileIndex.getInstance(module).deriveSchemaFromMigrations) {
      return null
    }

    return findDbFile()
  }
}
