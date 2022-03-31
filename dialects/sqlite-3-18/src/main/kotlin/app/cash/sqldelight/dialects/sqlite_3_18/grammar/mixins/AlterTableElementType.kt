package app.cash.sqldelight.dialects.sqlite_3_18.grammar.mixins

import app.cash.sqldelight.dialects.sqlite_3_18.grammar.psi.impl.SqliteAlterTableStmtImpl
import com.alecstrong.sql.psi.core.psi.SchemaContributorStub
import com.alecstrong.sql.psi.core.psi.mixins.AlterTableElementType
import com.alecstrong.sql.psi.core.psi.mixins.AlterTableStmtStub

internal class AlterTableElementType(
  name: String
) : AlterTableElementType("sqlite_3_18.$name") {
  override fun createPsi(stub: SchemaContributorStub) = SqliteAlterTableStmtImpl(
    stub as AlterTableStmtStub, this
  )
}
