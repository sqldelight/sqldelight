package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlExistsOperatorExpression
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode
import kotlin.text.endsWith

/**
 * The "?" exists operator is used by Ltree, Jsonb (not Json)
 * The type annotation is performed here for these types
 * For other json operators see JsonExpressionMixin
 */
internal abstract class ExistsOperatorExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqlBinaryExpr,
  PostgreSqlExistsOperatorExpression {

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val columnType = ((firstChild.firstChild.reference?.resolve() as? SqlColumnName)?.parent as? SqlColumnDef)?.columnType?.typeName?.text
    when {
      columnType == null ||
        columnType == "LTREE" ||
        columnType == "JSONB" ||
        columnType.endsWith("[]") -> super.annotate(annotationHolder)
      columnType == "JSON" -> annotationHolder.createErrorAnnotation(firstChild.firstChild, "Left side of jsonb expression must be a jsonb column.")
      else -> annotationHolder.createErrorAnnotation(firstChild.firstChild, "expression must be JSONB, LTREE.")
    }
  }
  override fun getExprList(): List<SqlExpr> {
    return children.filterIsInstance<SqlExpr>()
  }
}
