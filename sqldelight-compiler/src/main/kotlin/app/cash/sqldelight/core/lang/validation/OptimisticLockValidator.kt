package app.cash.sqldelight.core.lang.validation

import app.cash.sqldelight.core.lang.util.columnDefSource
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.core.lang.util.parentOfType
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.SqlCompilerAnnotator
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.Queryable
import com.alecstrong.sql.psi.core.psi.SqlBinaryAddExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryEqualityExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlUpdateStmt
import com.alecstrong.sql.psi.core.psi.SqlUpdateStmtLimited
import com.alecstrong.sql.psi.core.psi.mixins.ColumnDefMixin
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

open class OptimisticLockValidator : Annotator, SqlCompilerAnnotator {
  open fun quickFix(element: PsiElement, lock: ColumnDefMixin): IntentionAction? = null

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    annotate(element, holder, null)
  }

  override fun annotate(element: PsiElement, annotationHolder: SqlAnnotationHolder) {
    annotate(element, null, annotationHolder)
  }

  fun annotate(element: PsiElement, holder: AnnotationHolder?, sqlAnnotationHolder: SqlAnnotationHolder?) {
    val tableName = when (element) {
      is SqlUpdateStmt -> element.qualifiedTableName.tableName
      is SqlUpdateStmtLimited -> element.qualifiedTableName.tableName
      else -> return
    }

    val table = tableName.reference?.resolve()?.parentOfType<Queryable>()?.tableExposed() ?: return
    val lock = table.query.columns.mapNotNull { (it.element as NamedElement).columnDefSource() }.singleOrNull {
      it.columnType.node.getChildren(null).any { it.text == "LOCK" }
    } ?: return

    // Verify the update expression increments the lock.
    val (column, setter) = when (element) {
      is SqlUpdateStmt -> {
        val columns = element.columnNameList +
          element.updateStmtSubsequentSetterList.mapNotNull { it.columnName }
        val setters = element.setterExpressionList +
          element.updateStmtSubsequentSetterList.mapNotNull { it.setterExpression }
        columns.zip(setters)
      }
      is SqlUpdateStmtLimited -> {
        val columns = element.columnNameList +
          element.updateStmtSubsequentSetterList.mapNotNull { it.columnName }
        val setters = element.setterExpressionList +
          element.updateStmtSubsequentSetterList.mapNotNull { it.setterExpression }
        columns.zip(setters)
      }
      else -> throw IllegalStateException()
    }.singleOrNull { (column, _) -> column.textMatches(lock.columnName.name) } ?: (null to null)

    // Confirm the statement has SET lock = :arg + 1
    val updatesLock = column != null && setter != null &&
      setter.expr is SqlBinaryAddExpr &&
      setter.expr.node.getChildren(null).any { it.text == "+" } &&
      (setter.expr as SqlBinaryExpr).getExprList()[0] is SqlBindExpr &&
      (setter.expr as SqlBinaryExpr).getExprList()[1].textMatches("1")

    val whenExpression = when (element) {
      is SqlUpdateStmt -> element.expr
      is SqlUpdateStmtLimited -> element.exprList.getOrNull(0) ?: return
      else -> throw IllegalStateException()
    }

    // Confirms the statement has WHERE lock = :arg
    val queriesLock = whenExpression != null &&
      whenExpression.node.treePrev.treePrev.text == "WHERE" &&
      whenExpression.findChildrenOfType<SqlBinaryEqualityExpr>().any {
        (it.node.getChildren(null).any { it.text == "=" || it.text == "==" }) &&
          (it.getExprList()[0] as? SqlColumnExpr)?.columnName?.textMatches(lock.columnName.name) == true &&
          (it.getExprList()[1] is SqlBindExpr)
      }

    if (!queriesLock || !updatesLock) {
      if (holder != null) {
        val annotationBuilder = holder.newAnnotation(HighlightSeverity.ERROR, errorMessage)
          .range(element)

        quickFix(element, lock)?.let { annotationBuilder.withFix(it) }

        annotationBuilder.create()
      }

      sqlAnnotationHolder?.createErrorAnnotation(element, errorMessage)
    }
  }

  companion object {
    private const val errorMessage = "This query updates a table with an optimistic lock" +
      " but does not correctly use the lock."
  }
}
