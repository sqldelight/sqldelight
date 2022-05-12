package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.psi.SqlAlterTableRules
import com.alecstrong.sql.psi.core.psi.SqlAlterTableStmt
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlStmt

interface MigrationSquasher {
  fun squish(statement: SqlStmt, into: SqlFileBase): String
  fun squish(alterTableRules: SqlAlterTableRules, into: SqlFileBase): String
}

fun SqlAlterTableRules.alteredTable(file: SqlFileBase): SqlCreateTableStmt {
  val tableName = (parent as SqlAlterTableStmt).tableName
  return file.sqlStmtList!!.stmtList.mapNotNull { it.createTableStmt }.single { it.tableName.textMatches(tableName.text) }
}
