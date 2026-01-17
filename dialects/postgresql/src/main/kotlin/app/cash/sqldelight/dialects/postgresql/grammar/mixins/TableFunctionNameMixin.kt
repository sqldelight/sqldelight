package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import com.alecstrong.sql.psi.core.psi.SqlNamedElementImpl
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder

internal abstract class TableFunctionNameMixin(
  node: ASTNode,
) : SqlNamedElementImpl(node) {

  override val parseRule: (builder: PsiBuilder, level: Int) -> Boolean = PostgreSqlParser::unnest_table_function_real
}
