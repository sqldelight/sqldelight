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

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.rootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.core.lang.queriesName
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.psi.SqlDelightStmtIdentifier
import com.squareup.sqldelight.intellij.util.isAncestorOf
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class SqlDelightGotoDeclarationHandler : GotoDeclarationHandler {
  override fun getGotoDeclarationTargets(
    sourceElement: PsiElement?,
    offset: Int,
    editor: Editor
  ): Array<PsiElement> {
    if (sourceElement == null) return emptyArray()

    val elementFile = when(sourceElement.parent) {
      is PsiReference -> sourceElement.parent as PsiReference
      is KtNameReferenceExpression -> sourceElement.parent.references.firstIsInstance<KtSimpleNameReference>()
      else -> return emptyArray()
    }.resolve()?.containingFile?.virtualFile ?: return emptyArray()

    val module = elementFile.getModule(sourceElement.project) ?: return emptyArray()

    // Only handle files under the generated sqlite directory.
    val fileIndex = SqlDelightFileIndex.getInstance(module)
    if (!fileIndex.isConfigured) return emptyArray()

    val outputDirectory = fileIndex.contentRoot.findFileByRelativePath(fileIndex.outputDirectory) ?: return emptyArray()
    if (!outputDirectory.isAncestorOf(elementFile)) return emptyArray()

    var result = emptyArray<PsiElement>()
    module.rootManager.fileIndex.iterateContent { vFile ->
      if (vFile.fileType != SqlDelightFileType
          || vFile.queriesName != elementFile.nameWithoutExtension) {
        return@iterateContent true
      }
      val file = (PsiManager.getInstance(sourceElement.project).findFile(vFile) as SqlDelightFile)
      if (file.sqlStmtList == null) return@iterateContent false
      result = file.sqlStmtList!!
          .findChildrenOfType<SqlDelightStmtIdentifier>()
          .mapNotNull { it.identifier() }
          .filter { it.text == sourceElement.text }
          .toTypedArray()
      return@iterateContent false
    }

    return result
  }

  override fun getActionText(context: DataContext) = null
}
