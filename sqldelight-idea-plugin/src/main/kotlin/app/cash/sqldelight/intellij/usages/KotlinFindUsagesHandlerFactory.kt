package app.cash.sqldelight.intellij.usages

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

interface KotlinFindUsagesHandlerFactory {
  fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler
  val findFunctionOptions: FindUsagesOptions
  val findPropertyOptions: FindUsagesOptions
}

fun KotlinFindUsagesHandlerFactory(project: Project): KotlinFindUsagesHandlerFactory {
  return try {
    val factory = Class.forName("org.jetbrains.kotlin.idea.base.searching.usages.KotlinFindUsagesHandlerFactory")
    ReflectiveKotlinFindUsagesFactory(factory.getConstructor(Project::class.java).newInstance(project) as FindUsagesHandlerFactory)
  } catch (e: ClassNotFoundException) {
    LegacyKotlinFindUsagesFactory(org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory(project))
  }
}
