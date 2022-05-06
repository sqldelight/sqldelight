package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.psi.SqlAlterTableRules
import com.alecstrong.sql.psi.core.psi.SqlStmt

interface MigrationSquasher {
  fun squish(statement: SqlStmt, into: SqlFileBase): String
  fun squish(alterTableRules: SqlAlterTableRules, into: SqlFileBase): String
}
