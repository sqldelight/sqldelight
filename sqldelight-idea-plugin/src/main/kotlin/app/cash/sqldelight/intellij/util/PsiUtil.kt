/*
 * Copyright (C) 2018 Square, Inc.
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

package app.cash.sqldelight.intellij.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

internal inline fun <reified T : PsiElement> PsiElement.prevSiblingOfType(): T? =
  PsiTreeUtil.getPrevSiblingOfType(this, T::class.java)

internal inline fun <reified T : PsiElement> PsiFile.findElementOfTypeAtOffset(
  offset: Int
): T? {
  return PsiTreeUtil.findElementOfClassAtOffset(this, offset, T::class.java, false)
}
