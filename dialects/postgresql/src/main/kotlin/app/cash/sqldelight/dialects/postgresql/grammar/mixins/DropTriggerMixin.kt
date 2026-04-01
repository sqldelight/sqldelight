package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlDropTriggerStmt
import app.cash.sqldelight.dialects.postgresql.grammar.psi.impl.PostgreSqlDropTriggerStmtImpl
import com.alecstrong.sql.psi.core.SqlSchemaContributorElementType
import com.alecstrong.sql.psi.core.psi.SchemaContributorStub
import com.alecstrong.sql.psi.core.psi.SqlCreateTriggerStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.impl.SqlCreateTriggerStmtImpl
import com.alecstrong.sql.psi.core.psi.impl.SqlDropTriggerStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

internal abstract class DropTriggerMixin : SqlDropTriggerStmtImpl {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SchemaContributorStub, stubType: IStubElementType<*, *>) : super(stub, stubType)
}

internal class DropTriggerElementType(name: String) :
  SqlSchemaContributorElementType<PostgreSqlDropTriggerStmt>(
    name = "postgresql.$name",
    PostgreSqlDropTriggerStmt::class.java,
  ) {
  override fun nameType() = SqlTypes.TRIGGER_NAME
  override fun createPsi(stub: SchemaContributorStub) = PostgreSqlDropTriggerStmtImpl(stub, this)
}
