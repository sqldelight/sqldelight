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

import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.lang.QUERIES_SUFFIX_NAME
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.core.lang.queriesName
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.psi.SqlDelightStmtIdentifier
import com.squareup.sqldelight.intellij.util.isAncestorOf
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class SqlDelightGotoDeclarationHandler : GotoDeclarationHandler {
  override fun getGotoDeclarationTargets(
    sourceElement: PsiElement?,
    offset: Int,
    editor: Editor
  ): Array<PsiElement> {
    if (sourceElement == null) {
      return emptyArray()
    }

    val targetData = targetData(sourceElement) ?: return emptyArray()
    val function = targetData.function
    val elementFile = targetData.containingFile

    val module = elementFile.getModule(function.project) ?: return emptyArray()

    // Only handle files under the generated sqlite directory.
    val fileIndex = SqlDelightFileIndex.getInstance(module)
    if (!fileIndex.isConfigured) return emptyArray()

    fileIndex.outputDirectories().forEach { dir ->
      val outputDirectory = fileIndex.contentRoot.findFileByRelativePath(dir) ?: return emptyArray()
      if (!outputDirectory.isAncestorOf(elementFile)) return emptyArray()

      var result = emptyArray<PsiElement>()
      val scope = GlobalSearchScope.moduleWithDependentsScope(module)
      val manager = PsiManager.getInstance(function.project)
      FileTypeIndex.getFiles(SqlDelightFileType, scope)
        .asSequence()
        .filter { it.queriesName == elementFile.nameWithoutExtension }
        .map { manager.findFile(it) as SqlDelightFile }
        .filter { it.sqlStmtList != null }
        .forEach inner@{ sqlDelightFile ->
          val identifier = sqlDelightFile.sqlStmtList!!
            .findChildrenOfType<SqlDelightStmtIdentifier>()
            .mapNotNull { it.identifier() }
            .first { it.textMatches(function.name!!) }
          result = if (targetData.parameter != null) {
            val sqlStmt = identifier.getParentOfType<SqlDelightStmtIdentifier>(true)
              ?.getNextSiblingIgnoringWhitespaceAndComments() as? SqlStmt
              ?: return@inner

            sqlStmt.getChildOfType<QueryElement>()?.queryExposed()
              .orEmpty()
              .flatMap { it.columns }
              .filter { it.element.textMatches(targetData.parameter.name!!) }
              .mapNotNull { it.element.reference?.resolve() }
              .toTypedArray()
          } else {
            arrayOf(identifier)
          }
        }
      return result
    }

    return emptyArray()
  }

  private fun targetData(psiElement: PsiElement): TargetData? {
    val reference = when (psiElement.parent) {
      is PsiReference -> psiElement.parent as PsiReference
      is KtNameReferenceExpression -> {
        psiElement.parent.references.firstIsInstance<KtSimpleNameReference>()
      }
      else -> return null
    }
    return when (val resolved = reference.resolve()) {
      is KtParameter -> {
        val ktClass = resolved.containingClass() ?: return null
        val element = ReferencesSearch.search(ktClass)
          .mapping { it.element }
          .firstOrNull {
            it.containingFile.virtualFile.nameWithoutExtension.endsWith(QUERIES_SUFFIX_NAME)
          } ?: return null
        TargetData(
          parameter = resolved,
          function = element.getParentOfType(true)!!,
          containingFile = element.containingFile.virtualFile
        )
      }
      is KtNamedFunction, is PsiMethod -> TargetData(
        parameter = null,
        function = resolved as PsiNamedElement,
        containingFile = resolved.containingFile.virtualFile
      )
      else -> null
    }
  }

  override fun getActionText(context: DataContext): String? = null

  data class TargetData(
    val parameter: PsiNamedElement?,
    val function: PsiNamedElement,
    val containingFile: VirtualFile
  )
}
