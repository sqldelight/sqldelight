/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.intellij.psi

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
