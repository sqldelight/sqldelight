package app.cash.sqldelight.core.lang

import app.cash.sqldelight.core.SqlDelightFileIndex
import com.alecstrong.sql.psi.core.SqlFileBase
import com.intellij.psi.FileViewProvider

private const val VERSION_ALLOWED_SEPARATORS = ",:.-_T "

/**
 * Parses a migration version out of a filename
 *
 * A version is a string of characters starting with a digit and allowing any number of
 * [VERSION_ALLOWED_SEPARATORS] characters in between, e.g. `1-2-3`, `1..2.3`, `1_2_3` are all
 * parsed as the same number.
 * Any preceding or succeeding characters after the version are ignored.
 * If the version contains no digits at all, returns 0.
 */
internal fun String.toMigrationVersion(): Long {
  val start = indexOfFirst { it in '0'..'9' }
  if (start == -1) {
    // no digits
    return 0L
  }

  val len = substring(start)
    .indexOfFirst { it !in '0'..'9' && it !in VERSION_ALLOWED_SEPARATORS }
    .takeIf { it != -1 }
    ?: (length - start)

  return substring(start, start + len).filter { it in '0'..'9' }.toLongOrNull() ?: 0L
}

class MigrationFile(
  viewProvider: FileViewProvider,
) : SqlDelightFile(viewProvider, MigrationLanguage) {
  val version: Long by lazy {
    name.toMigrationVersion()
  }

  internal fun sqlStatements() = sqlStmtList!!.stmtList

  override val order
    get() = version

  override fun getFileType() = MigrationFileType

  override fun baseContributorFiles(): List<SqlFileBase> {
    val module = module
    if (module == null || SqlDelightFileIndex.getInstance(module).deriveSchemaFromMigrations) {
      return emptyList()
    }

    return listOfNotNull(findDbFile())
  }
}
