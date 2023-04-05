package app.cash.sqldelight.intellij.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.psi.PsiElement
import java.lang.reflect.Method

class ReflectiveKotlinFindUsagesFactory(
  private val wrapped: FindUsagesHandlerFactory
) : KotlinFindUsagesHandlerFactory {
  private val findFunctionOptionsMethod: Method = wrapped.javaClass.getMethod("getFindFunctionOptions")
  private val findPropertyOptionsMethod: Method = wrapped.javaClass.getMethod("getFindPropertyOptions")

  override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler {
    return wrapped.createFindUsagesHandler(element, forHighlightUsages)!!
  }

  override val findFunctionOptions get() = findFunctionOptionsMethod.invoke(wrapped) as FindUsagesOptions
  override val findPropertyOptions get() = findPropertyOptionsMethod.invoke(wrapped) as FindUsagesOptions
}
