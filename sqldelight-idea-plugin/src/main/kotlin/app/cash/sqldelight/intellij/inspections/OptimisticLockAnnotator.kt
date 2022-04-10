package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.core.lang.validation.OptimisticLockValidator
import app.cash.sqldelight.intellij.intentions.AddOptimisticLockIntention
import com.alecstrong.sql.psi.core.psi.mixins.ColumnDefMixin
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement

class OptimisticLockAnnotator : OptimisticLockValidator() {
  override fun quickFix(element: PsiElement, lock: ColumnDefMixin): IntentionAction? {
    return AddOptimisticLockIntention(element, lock)
  }
}
