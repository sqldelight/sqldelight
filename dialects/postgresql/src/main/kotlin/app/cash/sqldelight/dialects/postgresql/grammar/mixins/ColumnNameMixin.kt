package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAlterTableDropColumn
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.impl.SqlColumnNameImpl
import com.intellij.lang.ASTNode
/**
 * Allow column names to be annotated, for existence in schema, only when IF EXISTS is not present.
 * This allows for the following syntax to be valid:
 * ALTER TABLE test DROP COLUMN IF EXISTS to_drop;
 */
internal abstract class ColumnNameMixin(
  node: ASTNode,
) : SqlColumnNameImpl(node) {
  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val dropColumnParent = parent as? PostgreSqlAlterTableDropColumn
    // annotate (checks if column name exists) only when dropColumnParent is null or ifExists is null
    dropColumnParent?.ifExists ?: super.annotate(annotationHolder)
  }
}
