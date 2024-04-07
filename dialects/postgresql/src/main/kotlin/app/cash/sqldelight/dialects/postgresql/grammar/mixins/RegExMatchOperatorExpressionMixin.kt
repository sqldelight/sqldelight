package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlRegexMatchOperatorExpression
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
    val columnType = ((firstChild.firstChild.reference?.resolve() as? SqlColumnName)?.parent as? SqlColumnDef)?.columnType?.typeName?.text
    when {
      columnType == null -> super.annotate(annotationHolder)
      columnType != "TEXT" -> annotationHolder.createErrorAnnotation(firstChild.firstChild, """operator does not exist: $columnType ${regexMatchOperator.text} unknown""")
    }
    super.annotate(annotationHolder)
  }
  override fun getExprList(): List<SqlExpr> {
    return children.filterIsInstance<SqlExpr>()
  }
}
