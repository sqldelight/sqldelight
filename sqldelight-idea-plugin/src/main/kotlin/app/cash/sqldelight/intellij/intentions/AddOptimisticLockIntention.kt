package app.cash.sqldelight.intellij.intentions

import com.alecstrong.sql.psi.core.psi.SqlUpdateStmt
import com.alecstrong.sql.psi.core.psi.SqlUpdateStmtLimited
import com.alecstrong.sql.psi.core.psi.mixins.ColumnDefMixin
import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction
import com.intellij.codeInspection.HintAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ReadOnlyModificationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.inspections.findExistingEditor

class AddOptimisticLockIntention(
  private val updateElement: PsiElement,
  private val lock: ColumnDefMixin,
) : BaseElementAtCaretIntentionAction(), HintAction, QuestionAction {
  override fun getFamilyName() = INTENTIONS_FAMILY_NAME_LOCK_FIX

  override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
    text = "Add an optimistic lock to this update"
    return when (updateElement) {
      is SqlUpdateStmt -> updateElement.setterExpressionList.isNotEmpty()
      is SqlUpdateStmtLimited -> updateElement.setterExpressionList.isNotEmpty()
      else -> false
    }
  }

  override fun invoke(project: Project, editor: Editor, element: PsiElement) {
    val (lastSetter, subsequentSetters) = when (updateElement) {
      is SqlUpdateStmt -> updateElement.setterExpressionList.last() to updateElement.updateStmtSubsequentSetterList
      is SqlUpdateStmtLimited -> updateElement.setterExpressionList.last() to updateElement.updateStmtSubsequentSetterList
      else -> throw IllegalStateException()
    }

    val setterStart = if (subsequentSetters.isEmpty()) {
      lastSetter.textOffset + lastSetter.textLength
    } else {
      subsequentSetters.last().let { it.textOffset + it.textLength }
    }

    val whereClause = when (updateElement) {
      is SqlUpdateStmt -> updateElement.expr
      is SqlUpdateStmtLimited ->
        updateElement.exprList
          .single { it.node.treePrev.treePrev.text == "WHERE" }
      else -> throw IllegalStateException()
    }

    val queryStart =
      if (whereClause == null) setterStart
      else whereClause.textOffset + whereClause.textLength

    PsiDocumentManager.getInstance(project).commitAllDocuments()
    WriteCommandAction.writeCommandAction(project).run<ReadOnlyModificationException> {
      editor.document.addOptimisticLock(setterStart, whereClause != null, queryStart)
    }
  }

  override fun showHint(editor: Editor): Boolean {
    return false
  }

  override fun execute(): Boolean {
    invoke(updateElement.project, updateElement.findExistingEditor()!!, updateElement)
    return true
  }

  private fun Document.addOptimisticLock(setterStart: Int, hasWhereClause: Boolean, queryStart: Int) {
    val lockName = lock.columnName.name

    // Add the where clause first.
    if (hasWhereClause) {
      insertString(queryStart, " AND $lockName = :$lockName")
    } else {
      insertString(queryStart, "\nWHERE $lockName = :$lockName")
    }

    // Add the lock setter.
    insertString(setterStart, ",\n  $lockName = :$lockName + 1")
  }
}

private const val INTENTIONS_FAMILY_NAME_LOCK_FIX = "Add an optimistic lock check"
