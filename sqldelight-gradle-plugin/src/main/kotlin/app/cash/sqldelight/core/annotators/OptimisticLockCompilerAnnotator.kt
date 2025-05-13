package app.cash.sqldelight.core.annotators

import app.cash.sqldelight.core.lang.validation.OptimisticLockValidator
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.SqlCompilerAnnotator
import com.intellij.psi.PsiElement

class OptimisticLockCompilerAnnotator : SqlCompilerAnnotator {
  private val optimisticLockValidator = OptimisticLockValidator()

  override fun annotate(element: PsiElement, annotationHolder: SqlAnnotationHolder) {
    optimisticLockValidator.annotate(element, null, annotationHolder)
  }
}
