package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlRangeOperatorExpression
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

/**
 * Operators '<<' | '>>' | '&>' | '&<' | '-|-' for TsRange, TsTzRange, TSMULTIRANGE, TSTZMULTIRANGE
 */
internal abstract class RangeOperatorExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqlBinaryExpr,
  PostgreSqlRangeOperatorExpression {

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val columnType = ((firstChild.firstChild.reference?.resolve() as? SqlColumnName)?.parent as? SqlColumnDef)?.columnType?.typeName?.text
    when (columnType) {
      null, "TSRANGE", "TSTZRANGE", "TSMULTIRANGE", "TSTZMULTIRANGE" -> super.annotate(annotationHolder)
      else -> annotationHolder.createErrorAnnotation(firstChild.firstChild, "expression must be TSRANGE, TSTZRANGE, TSMULTIRANGE, TSTZMULTIRANGE")
    }
    super.annotate(annotationHolder)
  }
  override fun getExprList(): List<SqlExpr> {
    return children.filterIsInstance<SqlExpr>()
  }
}
