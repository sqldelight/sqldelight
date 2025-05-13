package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlRegexMatchOperatorExpression
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTypeName
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode
/**
 * Regular expression operators provide a more powerful means for pattern matching than the LIKE and SIMILAR TO operators.
 */
internal abstract class RegExMatchOperatorExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqlBinaryExpr,
  PostgreSqlRegexMatchOperatorExpression {

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    ((firstChild.firstChild.reference?.resolve() as? SqlColumnName)?.parent as? SqlColumnDef)?.isStringDataType()?.let { isText ->
      if (!isText) {
        annotationHolder.createErrorAnnotation(
          firstChild.firstChild,
          """operator ${regexMatchOperator.text} can only be performed on text""",
        )
      }
    }
    super.annotate(annotationHolder)
  }
  override fun getExprList(): List<SqlExpr> {
    return children.filterIsInstance<SqlExpr>()
  }

  private fun SqlColumnDef.isStringDataType(): Boolean {
    val typeName = columnType.typeName as PostgreSqlTypeName
    return typeName.stringDataType != null
  }
}
