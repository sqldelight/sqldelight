package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlContainsOperatorExpression
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

/**
 * The "@> <@" contain operators is used by Array, TsVector, Jsonb (not Json), TsRange, TsTzRange, TsMultiRange, TsTzMultiRange
 * The type annotation is performed here for these types
 * For other json operators see JsonExpressionMixin
 */
internal abstract class ContainsOperatorExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqlBinaryExpr,
  PostgreSqlContainsOperatorExpression {

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val columnType = ((firstChild.firstChild.reference?.resolve() as? SqlColumnName)?.parent as? SqlColumnDef)?.columnType?.typeName?.text
    when {
      columnType == null ||
        columnType == "JSONB" ||
        columnType == "TSVECTOR" ||
        columnType == "TSRANGE" ||
        columnType == "TSTZRANGE" ||
        columnType == "TSMULTIRANGE" ||
        columnType == "TSTZMULTIRANGE" ||
        columnType.endsWith("[]") -> super.annotate(annotationHolder)
      columnType == "JSON" -> annotationHolder.createErrorAnnotation(firstChild.firstChild, "Left side of jsonb expression must be a jsonb column.")
      else -> annotationHolder.createErrorAnnotation(firstChild.firstChild, "expression must be ARRAY, JSONB, TSVECTOR, TSRANGE, TSTZRANGE, TSMULTIRANGE, TSTZMULTIRANGE.")
    }
    super.annotate(annotationHolder)
  }
  override fun getExprList(): List<SqlExpr> {
    return children.filterIsInstance<SqlExpr>()
  }
}
