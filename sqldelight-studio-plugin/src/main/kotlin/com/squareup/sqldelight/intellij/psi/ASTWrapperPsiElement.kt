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

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import java.util.ArrayList

open class ASTWrapperPsiElement(node: ASTNode) : ASTWrapperPsiElement(
    node) {
  override fun getChildren(): Array<PsiElement> {
    var psiChild: PsiElement? = firstChild ?: return PsiElement.EMPTY_ARRAY

    val result = ArrayList<PsiElement>()
    while (psiChild != null) {
      result.add(psiChild)
      psiChild = psiChild.nextSibling
    }
    return PsiUtilCore.toPsiElementArray(result)
  }
}
