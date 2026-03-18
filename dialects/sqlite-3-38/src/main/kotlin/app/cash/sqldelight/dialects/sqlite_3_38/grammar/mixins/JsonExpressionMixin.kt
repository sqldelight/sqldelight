package app.cash.sqldelight.dialects.sqlite_3_38.grammar.mixins

import app.cash.sqldelight.dialects.sqlite_3_38.grammar.psi.SqliteJsonExpression
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.intellij.lang.ASTNode

internal abstract class JsonExpressionMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  SqlBinaryExpr,
  SqliteJsonExpression {
  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val columnType = ((firstChild as? SqlColumnExpr)?.columnName?.reference?.resolve()?.parent as? SqlColumnDef)?.columnType?.typeName?.text
    if (columnType == null || columnType != "TEXT") {
      annotationHolder.createErrorAnnotation(firstChild, "Left side of json expression must be a text column.")
    }
    super.annotate(annotationHolder)
  }

  override fun getExprList(): List<SqlExpr> {
    return children.filterIsInstance<SqlExpr>()
  }
}
