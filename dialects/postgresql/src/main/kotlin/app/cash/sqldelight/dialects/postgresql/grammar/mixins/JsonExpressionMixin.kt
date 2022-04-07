package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlJsonExpression
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode

internal abstract class JsonExpressionMixin(node: ASTNode) : SqlCompositeElementImpl(node),
  PostgreSqlJsonExpression {
  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val columnType = ((firstChild.reference?.resolve() as? SqlColumnName)?.parent as? SqlColumnDef)?.columnType?.typeName?.text
    if (columnType == null || columnType !in arrayOf("JSON", "JSONB")) {
      annotationHolder.createErrorAnnotation(firstChild, "Left side of json expression must be a json column.")
    }
    if (jsonbBinaryOperator != null && columnType != "JSONB") {
      annotationHolder.createErrorAnnotation(firstChild, "Left side of jsonb expression must be a jsonb column.")
    }
    super.annotate(annotationHolder)
  }
}