package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlCreateFunctionStmt
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.Schema
import com.alecstrong.sql.psi.core.psi.SchemaContributor
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlCreateTriggerStmt
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.mixins.SingleRow
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
/**
 * CreateFunctionMixin is to support Trigger functions.
 * The function links to the trigger using the function name
 */
internal abstract class CreateFunctionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  PostgreSqlCreateFunctionStmt,
  SchemaContributor {

  override fun modifySchema(schema: Schema) {
  }

  override fun name(): String {
    return findChildByType<SqlFunctionExpr>(SqlTypes.FUNCTION_EXPR)!!.text
  }

  override fun queryAvailable(child: PsiElement): Collection<QueryResult> {
    val functionName = findChildByType<SqlFunctionExpr>(SqlTypes.FUNCTION_EXPR)!!.text
    val trigger = containingFile.schema(SqlCreateTriggerStmt::class).find { it.node.findChildByType(SqlTypes.FUNCTION_EXPR)!!.text == functionName }
    val triggerTableName = trigger!!.tableName!!.name

    val triggerTableQuery = tablesAvailable(this).firstOrNull { it.tableName.name == triggerTableName }!!.query

    return listOf(
      QueryResult(
        SingleRow(triggerTableQuery.table!!, "new"),
        triggerTableQuery.columns,
        synthesizedColumns = triggerTableQuery.synthesizedColumns,
      ),
      QueryResult(
        SingleRow(triggerTableQuery.table!!, "old"),
        triggerTableQuery.columns,
        synthesizedColumns = triggerTableQuery.synthesizedColumns,
      ),
    )
  }
}
