package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlOverlapsOperatorExpression
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

/**
 * Overlaps operator '&&' for Array, TsRange, TsTzRange
 */
internal abstract class OverlapsOperatorExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqlBinaryExpr,
  PostgreSqlOverlapsOperatorExpression {

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val columnType = ((firstChild.firstChild.reference?.resolve() as? SqlColumnName)?.parent as? SqlColumnDef)?.columnType?.typeName?.text
    when {
      columnType == null || columnType == "TSRANGE" || columnType == "TSTZRANGE" || columnType == "TSMULTIRANGE" || columnType == "TSTZMULTIRANGE" -> super.annotate(annotationHolder)
      columnType.endsWith("[]") -> super.annotate(annotationHolder)
      else -> annotationHolder.createErrorAnnotation(firstChild.firstChild, "expression must be ARRAY, TSRANGE, TSTZRANGE, TSMULTIRANGE, TSTZMULTIRANGE.")
    }
    super.annotate(annotationHolder)
  }
  override fun getExprList(): List<SqlExpr> {
    return children.filterIsInstance<SqlExpr>()
  }
}
