package app.cash.sqldelight.intellij.inspections

import com.alecstrong.sql.psi.core.psi.SqlBinaryEqualityExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlLiteralExpr
import com.alecstrong.sql.psi.core.psi.SqlVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class NullEqualityInspection : LocalInspectionTool() {

  override fun buildVisitor(
    holder: ProblemsHolder,
    isOnTheFly: Boolean,
    session: LocalInspectionToolSession
  ): PsiElementVisitor {
    return object : SqlVisitor() {

      override fun visitBinaryEqualityExpr(o: SqlBinaryEqualityExpr) {
        val exprList = o.getExprList()
        if (exprList.size < 2) return
        val (expr1, expr2) = exprList
        if (expr1 is SqlColumnExpr && (expr2 is SqlLiteralExpr && expr2.literalValue.textMatches("NULL"))) {
          holder.registerProblem(o, "This comparison always evaluates to false, even for those rows where ${expr1.columnName.text} is NULL", ProblemHighlightType.WARNING)
        }
      }
    }
  }
}
