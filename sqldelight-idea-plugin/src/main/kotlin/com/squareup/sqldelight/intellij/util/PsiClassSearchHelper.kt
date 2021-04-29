package com.squareup.sqldelight.intellij.util

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import org.jetbrains.kotlin.idea.refactoring.memberInfo.qualifiedClassNameForRendering
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex

object PsiClassSearchHelper {

  fun getClassesByShortName(shortName: String, project: Project, scope: GlobalSearchScope): List<PsiClass> {
    val javaPsiFacade = JavaPsiFacade.getInstance(project)

    val kotlinClasses = KotlinClassShortNameIndex.getInstance()
      .get(shortName, project, scope)
      .mapNotNull { javaPsiFacade.findClass(it.qualifiedClassNameForRendering(), scope) }

    val javaClasses = PsiShortNamesCache.getInstance(project)
      .getClassesByName(shortName, scope)
    return (kotlinClasses + javaClasses)
      .distinctBy { it.qualifiedName }
  }
}
