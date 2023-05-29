package app.cash.sqldelight.intellij.inspections

import com.alecstrong.sql.psi.core.psi.SqlDeleteStmt
import com.alecstrong.sql.psi.core.psi.SqlDeleteStmtLimited
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.SqlUpdateStmt
import com.alecstrong.sql.psi.core.psi.SqlUpdateStmtLimited
import com.alecstrong.sql.psi.core.psi.SqlVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil.processElements
import com.intellij.psi.util.elementType

internal class MissingWhereInspection : LocalInspectionTool() {
  override fun buildVisitor(
    holder: ProblemsHolder,
    isOnTheFly: Boolean,
    session: LocalInspectionToolSession,
  ): PsiElementVisitor = ensureReady(session.file) {
    object : SqlVisitor() {
      override fun visitDeleteStmt(o: SqlDeleteStmt) =
        ignoreInvalidElements { check(o, holder) }

      override fun visitDeleteStmtLimited(o: SqlDeleteStmtLimited) =
        ignoreInvalidElements { check(o, holder) }

      override fun visitUpdateStmt(o: SqlUpdateStmt) =
        ignoreInvalidElements { check(o, holder) }

      override fun visitUpdateStmtLimited(o: SqlUpdateStmtLimited) =
        ignoreInvalidElements { check(o, holder) }
    }
  }

  private fun check(o: PsiElement, holder: ProblemsHolder) {
    val keyword = when (o) {
      is SqlUpdateStmt, is SqlUpdateStmtLimited -> "overwrite"
      is SqlDeleteStmt, is SqlDeleteStmtLimited -> "delete"
      else -> throw AssertionError()
    }
    val element = o.findElementWithType(SqlTypes.WHERE)
    if (element == null) {
      holder.registerProblem(
        o,
        "This query may $keyword all records, consider adding a WHERE clause",
        ProblemHighlightType.WARNING,
      )
    }
  }
}

private fun PsiElement.findElementWithType(type: IElementType): PsiElement? {
  val processor = object : PsiElementProcessor.FindElement<PsiElement>() {
    override fun execute(element: PsiElement): Boolean {
      if (element.elementType == type) {
        setFound(element)
      }
      return true
    }
  }
  processElements(this, processor)
  return processor.foundElement
}
