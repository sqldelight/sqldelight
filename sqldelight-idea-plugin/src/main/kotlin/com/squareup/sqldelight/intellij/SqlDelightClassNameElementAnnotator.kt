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
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.psi.ImportStmtMixin
import com.squareup.sqldelight.core.lang.psi.JavaTypeMixin
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.intellij.intentions.AddImportIntention
import com.squareup.sqldelight.intellij.util.PsiClassSearchHelper

class SqlDelightClassNameElementAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element !is JavaTypeMixin || element.reference.resolve() != null) {
      return
    }

    val project = element.project
    val sqlDelightFile = element.containingFile as SqlDelightFile
    val module = ModuleUtil.findModuleForFile(sqlDelightFile) ?: return
    val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
    val outerClassElement = element.firstChild
    val outerClassName = outerClassElement.text
    val classes = PsiClassSearchHelper.getClassesByShortName(outerClassName, project, scope)

    val hasImport = sqlDelightFile.hasImport(outerClassName)
    val psiElement = if (hasImport) {
      missingNestedClass(classes, element)
    } else {
      outerClassElement
    }

    val needsQuickFix = classes.isNotEmpty() && !hasImport

    holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved reference: ${psiElement.text}")
      .range(psiElement)
      .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
      .apply {
        if (needsQuickFix) {
          withFix(AddImportIntention(outerClassName))
        }
      }
      .create()
  }

  private fun SqlDelightFile.hasImport(outerClassName: String): Boolean {
    return sqlStmtList?.findChildrenOfType<ImportStmtMixin>()
      .orEmpty()
      .any { it.javaType.text.endsWith(outerClassName) }
  }

  private fun missingNestedClass(classes: List<PsiClass>, javaTypeMixin: JavaTypeMixin): PsiElement {
    val elementText = javaTypeMixin.text
    val className = classes.map { clazz -> findMissingNestedClassName(clazz, elementText) }
      .maxByOrNull { it.length }
      ?.substringBefore(".") ?: return javaTypeMixin.firstChild
    return javaTypeMixin.findChildrenOfType<PsiElement>().first { it.textMatches(className) }
  }

  private fun findMissingNestedClassName(psiClass: PsiClass, className: String): String {
    val nestedClassName = className.substringBefore(".")
    if (psiClass.name != nestedClassName) {
      return nestedClassName
    }
    val nextName = className.removePrefix(nestedClassName).removePrefix(".")
    val lookupString = nextName.substringBefore(".")
    val nextClass = psiClass.innerClasses.firstOrNull { clazz ->
      clazz.textMatches(lookupString)
    }
    return nextClass?.let { findMissingNestedClassName(it, nextName) } ?: nextName
  }
}
