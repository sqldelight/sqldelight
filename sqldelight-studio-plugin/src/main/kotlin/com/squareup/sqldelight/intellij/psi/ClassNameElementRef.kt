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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.squareup.sqldelight.intellij.lang.SqliteFile

class ClassNameElementRef(element: ClassNameElement, private val className: String)
: PsiReferenceBase<ClassNameElement>(element, TextRange(0, className.length)) {
  override fun resolve(): PsiElement? {
    var result = JavaPsiFacade.getInstance(element.project).findClass(className,
      findModuleForPsiElement(element)!!.getModuleWithDependenciesAndLibrariesScope(false))
    if (result != null) {
      return result;
    }
    (element.containingFile as SqliteFile).parseThen({ parsed ->
      parsed.sql_stmt_list().import_stmt().map { it.java_type_name() }.forEach {
        if (it.text.endsWith(element.text.substringBefore('.'))) {
          result = JavaPsiFacade.getInstance(element.project).findClass(
              it.text.substringBefore(element.text.substringBefore('.')) + element.text,
              findModuleForPsiElement(element)!!.getModuleWithDependenciesAndLibrariesScope(false)
          )
        }
      }
    })
    return result
  }

  override fun getVariants() = emptyArray<Any>()
}
