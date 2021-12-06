package app.cash.sqldelight.intellij.inspections.suppression

import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.psi.StmtIdentifier
import app.cash.sqldelight.core.psi.SqlDelightImportStmt
import app.cash.sqldelight.core.psi.SqlDelightImportStmtList
import app.cash.sqldelight.core.psi.SqlDelightStmtIdentifier
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlStmtList
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.SuppressionUtil
import com.intellij.codeInspection.SuppressionUtil.SUPPRESS_INSPECTIONS_TAG_NAME
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentOfTypes
import java.util.regex.Pattern

private val PsiElement.prevVisibleSiblingSkippingIdentifier: PsiElement?
  get() = generateSequence(prevSibling) { it.prevSibling }
    .filter { it !is PsiWhiteSpace && it !is StmtIdentifier }
    .firstOrNull()

private val PsiElement.isFirst: Boolean
  get() {
    val element = prevVisibleSiblingSkippingIdentifier
    return element == null || element is SqlDelightImportStmtList
  }

internal val PsiElement.comment: PsiElement?
  get() = prevVisibleSiblingSkippingIdentifier?.takeIf { it.elementType == SqlTypes.COMMENT }

internal val commentPattern: Pattern = Pattern.compile("--\\s*$SUPPRESS_INSPECTIONS_TAG_NAME\\s+(${LocalInspectionTool.VALID_ID_PATTERN}(\\s*,\\s*${LocalInspectionTool.VALID_ID_PATTERN})*)\\s*\\w*.*")

internal class SqlDelightInspectionSuppressor : InspectionSuppressor {
  override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
    if (element is SqlDelightFile) return false

    val stmt = element.parentOfTypes(
      classes = arrayOf(
        SqlStmt::class,
        SqlDelightImportStmt::class,
        SqlDelightStmtIdentifier::class
      ),
      withSelf = true
    ) ?: return false

    val comment = stmt.comment ?: run {
      if (stmt.isFirst) {
        stmt.parentOfType<SqlStmtList>()?.comment
      } else {
        null
      }
    } ?: return false

    val matcher = commentPattern.matcher(comment.text)
    return matcher.matches() && SuppressionUtil.isInspectionToolIdMentioned(
      matcher.group(1),
      toolId
    )
  }

  override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
    return arrayOf(SqlDelightSuppressInspectionQuickFix(toolId))
  }
}
