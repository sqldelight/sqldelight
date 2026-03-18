package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlForLockingReferenceName
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.SqlParser
import com.alecstrong.sql.psi.core.psi.FromQuery
import com.alecstrong.sql.psi.core.psi.SqlCompoundSelectStmt
import com.alecstrong.sql.psi.core.psi.SqlNamedElementImpl
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.util.parentOfType

internal abstract class ForLockingReferenceMixin(
  node: ASTNode,
) : SqlNamedElementImpl(node),
  PostgreSqlForLockingReferenceName {

  override val parseRule: (PsiBuilder, Int) -> Boolean = SqlParser::table_name_real

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val targetName = name

    val compoundSelect = parentOfType<SqlCompoundSelectStmt>(withSelf = false) ?: return
    val availableTables = compoundSelect.selectStmtList
      .flatMap { (it as? FromQuery)?.fromQuery().orEmpty() }
      .mapNotNull { it.table?.name }
      .toSet()

    if (targetName !in availableTables) {
      annotationHolder.createErrorAnnotation(
        this,
        "No table found in the FROM clause with name $targetName",
      )
    }

    super.annotate(annotationHolder)
  }
}
