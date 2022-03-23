package app.cash.sqldelight.intellij.refactoring.strategy

import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_18
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_24
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_25
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_35

interface SqlGeneratorStrategy {
  fun tableNameChanged(oldName: String, newName: String): String
  fun columnAdded(tableName: String, columnDef: String): String
  fun columnRemoved(tableName: String, columnName: String, columnDefList: List<String>): String
  fun columnNameChanged(
    tableName: String,
    oldName: String,
    newName: String,
    columnDefList: List<String>
  ): String

  companion object Factory {
    fun create(dialect: DialectPreset): SqlGeneratorStrategy {
      return when (dialect) {
        SQLITE_3_18, SQLITE_3_24, SQLITE_3_25, SQLITE_3_35 -> Sqlite()
        // MYSQL -> TODO()
        // POSTGRESQL -> TODO()
        // HSQL -> TODO()
        else -> NoOp()
      }
    }
  }
}
