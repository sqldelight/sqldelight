package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.core.lang.util.findChildrenOfType
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlValuesExpression
import com.alecstrong.sql.psi.core.psi.SqlVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

private val positional = "^\\?\\d*\$".toRegex()
private val named = "^[:@$][a-zA-Z0-9]*\$".toRegex()

internal class MixedNamedAndPositionalParamsInspection : LocalInspectionTool() {

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SqlVisitor() {
      override fun visitExpr(o: SqlExpr) {
        if (o !is SqlBinaryExpr) return

        checkExpression(o)
      }

      override fun visitValuesExpression(o: SqlValuesExpression) {
        checkExpression(o)
      }

      private fun checkExpression(o: SqlAnnotatedElement) {
        val bindExprs = o.findChildrenOfType<SqlBindExpr>()
        if (bindExprs.size < 2) {
          return
        }
        val params = bindExprs.map { it.bindParameter.text }
        val positional = params.filter { it.matches(positional) }
        val named = params.filter { it.matches(named) }
        if (positional.isNotEmpty() && named.isNotEmpty()) {
          holder.registerProblem(
            o,
            "Mixed usage of named and positional parameters is not recommended",
            ProblemHighlightType.WARNING
          )
        }
      }
    }
  }
}
