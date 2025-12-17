package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlDropFunctionStmt
import com.alecstrong.sql.psi.core.psi.Schema
import com.alecstrong.sql.psi.core.psi.SchemaContributor
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.lang.ASTNode

internal abstract class DropFunctionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  PostgreSqlDropFunctionStmt,
  SchemaContributor {

  override fun modifySchema(schema: Schema) {
  }

  override fun name(): String {
    return findChildByType<SqlFunctionExpr>(SqlTypes.FUNCTION_EXPR)!!.text
  }
}
