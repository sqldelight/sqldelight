package com.squareup.sqldelight.intellij.inspections

import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlIsExpr
import com.alecstrong.sql.psi.core.psi.SqlSelectStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.SqlVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType

internal class RedundantNullCheckInspection : LocalInspectionTool() {

  override fun buildVisitor(
    holder: ProblemsHolder,
    isOnTheFly: Boolean,
    session: LocalInspectionToolSession
  ): PsiElementVisitor = object : SqlVisitor() {

    override fun visitIsExpr(o: SqlIsExpr) {
      if (o.context !is SqlSelectStmt) return
      if (PsiTreeUtil.prevVisibleLeaf(o).elementType != SqlTypes.WHERE) return

      val firstChild = o.firstChild
      if (firstChild !is SqlColumnExpr) return
      val clauseText = o.text
      if (!clauseText.endsWith("IS NOT NULL") && !clauseText.endsWith("IS NULL")) return

      val columnName = firstChild.columnName
      val sqlColumnDef = columnName.reference?.resolve()?.parentOfType<SqlColumnDef>() ?: return
      val hasNotNullConstraint = sqlColumnDef.columnConstraintList.any { constraint ->
        constraint.textMatches("NOT NULL")
      }
      if (!hasNotNullConstraint) {
        return
      }

      holder.registerProblem(o, "Column ${columnName.name} defined as NOT NULL", ProblemHighlightType.WARNING)
    }
  }
}
