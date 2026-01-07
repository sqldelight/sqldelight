package app.cash.sqldelight.dialects.postgresql.grammar.mixins

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

internal class DropTriggerElementType(name: String) : SqlSchemaContributorElementType<SqlCreateTriggerStmt>(name, SqlCreateTriggerStmt::class.java) {
  override fun nameType() = SqlTypes.TRIGGER_NAME
  override fun createPsi(stub: SchemaContributorStub) = SqlCreateTriggerStmtImpl(stub, this)
}
