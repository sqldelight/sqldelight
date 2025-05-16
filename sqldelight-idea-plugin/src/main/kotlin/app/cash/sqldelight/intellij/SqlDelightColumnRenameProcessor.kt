package app.cash.sqldelight.intellij

import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.application.ReadActionProcessor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.CommonProcessors

class SqlDelightColumnRenameProcessor : RenamePsiElementProcessor() {

  private val findUsagesHandlerFactory = SqlDelightFindUsagesHandlerFactory()
  override fun canProcessElement(element: PsiElement): Boolean {
    return element is SqlColumnName && findUsagesHandlerFactory.canFindUsages(element)
  }

  override fun findReferences(
    element: PsiElement,
    searchScope: SearchScope,
    searchInCommentsAndStrings: Boolean,
  ): Collection<PsiReference> {
    if (element !is SqlColumnName) {
      return super.findReferences(element, searchScope, searchInCommentsAndStrings)
    }

    val collectProcessor = CommonProcessors.CollectProcessor<UsageInfo>()
    val readActionProcessor = ReadActionProcessor.wrapInReadAction(collectProcessor)
    val findUsagesHandler = findUsagesHandlerFactory.createFindUsagesHandler(element, false)
    findUsagesHandler?.processElementUsages(
      /* element = */
      element,
      /* processor = */
      readActionProcessor,
      /* options = */
      FindUsagesOptions(element.project).apply {
        isUsages = true
        isSearchForTextOccurrences = false
      },
    )
    return collectProcessor.results.mapNotNull { it.reference }
  }
}
