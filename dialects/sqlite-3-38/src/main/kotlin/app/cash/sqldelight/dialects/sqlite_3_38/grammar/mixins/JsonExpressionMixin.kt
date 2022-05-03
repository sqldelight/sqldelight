package app.cash.sqldelight.dialects.sqlite_3_38.grammar.mixins

import app.cash.sqldelight.dialects.sqlite_3_38.grammar.psi.SqliteJsonExpression
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode

internal abstract class JsonExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqliteJsonExpression {
  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val columnType = ((firstChild.reference?.resolve() as? SqlColumnName)?.parent as? SqlColumnDef)?.columnType?.typeName?.text
    if (columnType == null || columnType != "TEXT") {
      annotationHolder.createErrorAnnotation(firstChild, "Left side of json expression must be a text column.")
    }
    super.annotate(annotationHolder)
  }
}
