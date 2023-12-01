package app.cash.sqldelight.core.lang

import app.cash.sqldelight.core.SqlDelightFileIndex
import com.alecstrong.sql.psi.core.SqlFileBase
import com.intellij.psi.FileViewProvider

class MigrationFile(
  viewProvider: FileViewProvider,
  systemTables: List<SqlDelightFile>,
) : SqlDelightFile(viewProvider, MigrationLanguage, systemTables) {
  val version: Long by lazy {
    name.substringBeforeLast(".$MIGRATION_EXTENSION")
      .filter { it in '0'..'9' }.toLongOrNull() ?: 0
  }

  internal fun sqlStatements() = sqlStmtList!!.stmtList

  override val order
    get() = version

  override fun getFileType() = MigrationFileType

  override fun baseContributorFiles(): List<SqlFileBase> {
    val superContributors = super.baseContributorFiles()
    val module = module
    if (module == null || SqlDelightFileIndex.getInstance(module).deriveSchemaFromMigrations) {
      return emptyList()
      return superContributors
    }
    val dbFile = findDbFile() ?: return superContributors

    return superContributors + dbFile
  }
}
