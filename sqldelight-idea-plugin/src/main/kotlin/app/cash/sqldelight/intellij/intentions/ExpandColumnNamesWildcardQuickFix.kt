package app.cash.sqldelight.intellij.intentions

import com.alecstrong.sql.psi.core.psi.SqlResultColumn
import com.alecstrong.sql.psi.core.psi.SqlSelectStmt
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ReadOnlyModificationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ExpandColumnNamesWildcardQuickFix : BaseElementAtCaretIntentionAction() {

  override fun getFamilyName() = INTENTIONS_FAMILY_NAME_REFACTORINGS

  override fun getText() = INTENTION_EXPAND_COLUMN_NAMES_TEXT
  override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
    val selectStmt = PsiTreeUtil.getParentOfType(element, SqlSelectStmt::class.java)
      ?: return false
    return selectStmt.resultColumnList.any { it.textContains('*') && it.expr == null }
  }

  override fun invoke(project: Project, editor: Editor, element: PsiElement) {
    val selectStmt = PsiTreeUtil.getParentOfType(element, SqlSelectStmt::class.java) ?: return

    val allColumns = selectStmt.resultColumnList
      .groupBy({ it.findTableName().orEmpty() }, { it.findColumns() })
      .flatMap { (key: String, value: List<List<String>>) ->
        value.flatten().distinct()
          .map {
            buildString {
              if (key.isNotEmpty()) {
                append(key)
                append(".")
              }
              append(it)
            }
          }
      }
      .joinToString()

    val (start, end) = selectStmt.resultColumnList.startEndOffset()
    WriteCommandAction.writeCommandAction(project).run<ReadOnlyModificationException> {
      editor.document.replaceString(start, end, allColumns)
    }
  }

  private fun SqlResultColumn.findTableName() =
    PsiTreeUtil.findChildOfType(this, SqlTableName::class.java)?.name

  private fun SqlResultColumn.findColumns(): List<String> =
    queryExposed().flatMap { result -> result.columns.map { it.element.text } }

  private fun List<SqlResultColumn>.startEndOffset(): Pair<Int, Int> {
    var start = firstOrNull()?.startOffset ?: 0
    var end = firstOrNull()?.endOffset ?: 0
    for (i in 1..lastIndex) {
      start = minOf(start, get(i).startOffset)
      end = maxOf(end, get(i).endOffset)
    }
    return start to end
  }
}
