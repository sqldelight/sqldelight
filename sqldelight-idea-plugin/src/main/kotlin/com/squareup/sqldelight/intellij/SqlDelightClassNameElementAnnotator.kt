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
package com.squareup.sqldelight.intellij

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.squareup.sqldelight.core.lang.psi.JavaTypeMixin
import com.squareup.sqldelight.intellij.intentions.AddImportIntention

class SqlDelightClassNameElementAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element !is JavaTypeMixin || element.reference.resolve() != null) {
      return
    }

    holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved reference: ${element.text}")
      .range(element)
      .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
      .apply {
        val classes = PsiShortNamesCache.getInstance(element.project)
          .getClassesByName(element.text, GlobalSearchScope.allScope(element.project))
        if (classes.isNotEmpty()) {
          withFix(AddImportIntention(element.text))
        }
      }
      .create()
  }
}
