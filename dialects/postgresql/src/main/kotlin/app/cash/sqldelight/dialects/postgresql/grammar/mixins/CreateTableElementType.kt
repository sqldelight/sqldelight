package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.impl.PostgreSqlCreateTableStmtImpl
import com.alecstrong.sql.psi.core.psi.SchemaContributorStub
import com.alecstrong.sql.psi.core.psi.mixins.CreateTableElementType

internal class CreateTableElementType(
  name: String,
) : CreateTableElementType("postgresql.$name") {
  override fun createPsi(stub: SchemaContributorStub) = PostgreSqlCreateTableStmtImpl(stub, this)
}
