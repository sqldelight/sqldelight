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
package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.psi.JavaTypeMixin
import app.cash.sqldelight.core.lang.util.findChildOfType
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.core.psi.SqlDelightImportStmt
import app.cash.sqldelight.core.psi.SqlDelightImportStmtList
import app.cash.sqldelight.intellij.intentions.AddImportIntention
import app.cash.sqldelight.intellij.util.PsiClassSearchHelper
import app.cash.sqldelight.intellij.util.PsiClassSearchHelper.ImportableType
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope

internal data class AnnotationData(
  val element: PsiElement,
  val intentionAvailable: Boolean,
  val classes: List<ImportableType> = emptyList(),
)

internal class SqlDelightClassNameElementAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element !is JavaTypeMixin || element.reference.resolve() != null) {
      return
    }
    val data = if (element.context is SqlDelightImportStmt) {
      AnnotationData(element, false)
    } else {
      val project = element.project
      val sqlDelightFile = element.containingFile as SqlDelightFile
      val module = ModuleUtil.findModuleForFile(sqlDelightFile) ?: return
      val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
      val outerClassElement = element.firstChild
      val outerClassName = outerClassElement.text
      val classes = PsiClassSearchHelper.getClassesByShortName(outerClassName, project, scope)
      val hasImport = sqlDelightFile.hasImport(outerClassName)
      val enabled = classes.isNotEmpty() && !hasImport
      val psiElement = if (hasImport) {
        missingNestedClass(classes, element)
      } else {
        outerClassElement
      }
      AnnotationData(psiElement, enabled, classes)
    }

    holder.newAnnotation(HighlightSeverity.ERROR, "Unresolved reference: ${data.element.text}")
      .range(data.element)
      .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
      .withFix(AddImportIntention(data.element, data.classes, data.intentionAvailable))
      .create()
  }

  private fun SqlDelightFile.hasImport(outerClassName: String): Boolean {
    return sqlStmtList?.findChildOfType<SqlDelightImportStmtList>()
      ?.importStmtList
      .orEmpty()
      .any { it.javaType.text.endsWith(outerClassName) }
  }

  private fun missingNestedClass(
    classes: List<ImportableType>,
    javaTypeMixin: JavaTypeMixin
  ): PsiElement {
    val elementText = javaTypeMixin.text
    val className = classes.map { clazz -> findMissingNestedClassName(clazz, elementText) }
      .maxBy { it.length }
      ?.substringBefore(".") ?: return javaTypeMixin.firstChild
    return javaTypeMixin.findChildrenOfType<PsiElement>().first { it.textMatches(className) }
  }

  private fun findMissingNestedClassName(psiClass: ImportableType, className: String): String {
    val nestedClassName = className.substringBefore(".")
    if (psiClass.name != nestedClassName) {
      return nestedClassName
    }
    val nextName = className.substringAfter(".")
    val lookupString = nextName.substringBefore(".")
    val nextClass = psiClass.innerClasses.firstOrNull { clazz ->
      clazz.name == lookupString
    }
    return nextClass?.let { findMissingNestedClassName(it, nextName) } ?: nextName
  }
}
