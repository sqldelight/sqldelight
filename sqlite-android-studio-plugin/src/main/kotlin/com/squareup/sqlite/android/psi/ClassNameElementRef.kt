package com.squareup.sqlite.android.psi

import com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiReferenceBase

class ClassNameElementRef(element: ClassNameElement, className: String)
: PsiReferenceBase<ClassNameElement>(element, TextRange(1, className.length - 1)) {
  private val className =
      if (className[0] == '\'') className.substring(1, className.length - 1) else className

  override fun resolve() = JavaPsiFacade.getInstance(element.project).findClass(className,
      findModuleForPsiElement(element)!!.getModuleWithDependenciesAndLibrariesScope(false))

  override fun getVariants() = emptyArray<Any>()
}
