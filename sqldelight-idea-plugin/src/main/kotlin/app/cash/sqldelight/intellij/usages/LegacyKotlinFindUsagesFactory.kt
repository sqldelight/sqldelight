package app.cash.sqldelight.intellij.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.psi.PsiElement

class LegacyKotlinFindUsagesFactory(
  private val wrapped: org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
) : KotlinFindUsagesHandlerFactory {
  override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler {
    return wrapped.createFindUsagesHandler(element, forHighlightUsages)
  }
  override val findFunctionOptions get() = wrapped.findFunctionOptions
  override val findPropertyOptions get() = wrapped.findPropertyOptions
}
