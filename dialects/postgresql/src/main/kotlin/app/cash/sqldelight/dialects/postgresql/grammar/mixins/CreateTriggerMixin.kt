package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import com.alecstrong.sql.psi.core.SqlSchemaContributorElementType
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SchemaContributorStub
import com.alecstrong.sql.psi.core.psi.SqlCreateTriggerStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.impl.SqlCreateTriggerStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
/**
 * This PostgreSql CreateTriggerStmt extends SqlCreateTriggerStmtImpl allows access to sql-psi internals.
 * PostgreSql uses a Function for the trigger to execute DML - so the inherited implementation for DML not used.
 * See CreateFunctionMixin.
 */
internal abstract class CreateTriggerMixin : SqlCreateTriggerStmtImpl {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SchemaContributorStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

  override fun queryAvailable(child: PsiElement): Collection<QueryElement.QueryResult> {
    val tableName = tableName
    return listOfNotNull(
      tablesAvailable(this).firstOrNull { it.tableName.name == tableName!!.text }?.query,
    )
  }
}

internal class CreateTriggerElementType(name: String) : SqlSchemaContributorElementType<SqlCreateTriggerStmt>(name, SqlCreateTriggerStmt::class.java) {
  override fun nameType() = SqlTypes.TRIGGER_NAME
  override fun createPsi(stub: SchemaContributorStub) = SqlCreateTriggerStmtImpl(stub, this)
}
