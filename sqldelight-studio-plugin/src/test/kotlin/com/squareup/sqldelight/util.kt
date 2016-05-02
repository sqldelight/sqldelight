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
package com.squareup.sqldelight

import com.google.common.truth.Subject
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertWithMessage
import com.intellij.psi.PsiElement

internal class PsiElementSubject(
    private val element: PsiElement
) : Subject<PsiElementSubject, PsiElement>(Truth.THROW_ASSERTION_ERROR, element) {
  fun isAtPosition(file: String, offset: Int) {
    assertWithMessage("Wrong file name").that(element.containingFile.name).isEqualTo(file)
    assertWithMessage("Wrong offset").that(element.textOffset).isEqualTo(offset)
  }
}

internal fun PsiElement.assertThat() = PsiElementSubject(this)