package com.squareup.sqldelight.intellij.inspections

import com.alecstrong.sql.psi.core.psi.SqlBinaryEqualityExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlJoinConstraint
import com.alecstrong.sql.psi.core.psi.SqlParenExpr
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import com.squareup.sqldelight.core.lang.psi.isColumnSameAs
import com.squareup.sqldelight.core.lang.psi.isTypeSameAs
import com.squareup.sqldelight.core.lang.util.findChildrenOfType

class MismatchJoinColumnInspection : LocalInspectionTool() {
  override fun checkFile(
    file: PsiFile,
    manager: InspectionManager,
    isOnTheFly: Boolean
  ): Array<ProblemDescriptor> {
    val joinConstraints = file.findChildrenOfType<SqlJoinConstraint>()
      .mapNotNull { it.expr?.binaryEqualityExpr() }

    return joinConstraints.mapNotNull { joinEquality ->
      val exprList = joinEquality.getExprList()
      if (exprList.size < 2) return@mapNotNull null
      val (expr1, expr2) = exprList
      if (expr1 !is SqlColumnExpr || expr2 !is SqlColumnExpr) return@mapNotNull null

      val column1 = expr1.columnName
      val column2 = expr2.columnName

      if (column1.isColumnSameAs(column2)) {
        return@mapNotNull manager.createProblemDescriptor(
          joinEquality, "Join condition always evaluates to true", isOnTheFly,
          emptyArray(), ProblemHighlightType.WARNING
        )
      }

      if (!column1.isTypeSameAs(column2)) {
        return@mapNotNull manager.createProblemDescriptor(
          joinEquality, "Join compares two columns of different types", isOnTheFly,
          emptyArray(), ProblemHighlightType.WARNING
        )
      }
      return@mapNotNull null
    }.toTypedArray()
  }

  private fun SqlExpr.binaryEqualityExpr(): SqlBinaryEqualityExpr? = when (this) {
    is SqlParenExpr -> expr?.binaryEqualityExpr()
    is SqlBinaryEqualityExpr -> this
    else -> null
  }
}
