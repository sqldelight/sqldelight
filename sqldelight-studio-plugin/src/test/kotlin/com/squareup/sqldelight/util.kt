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

import com.google.common.truth.IterableSubject
import com.google.common.truth.Subject
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertWithMessage
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import java.io.File

internal class PsiElementSubject(
    private val element: PsiElement
) : Subject<PsiElementSubject, PsiElement>(Truth.THROW_ASSERTION_ERROR, element) {
  fun isAtPosition(file: String, offset: Int) {
    assertWithMessage("Wrong file name").that(element.containingFile.name).isEqualTo(File(file).name)
    assertWithMessage("Wrong offset").that(element.textOffset).isEqualTo(offset)
  }

  fun isAtCaret(file: String, caret: String) {
    isAtPosition(file, SqlDelightFixtureTestCase.getPosition(file, caret))
  }
}

internal fun PsiElement.assertThat() = PsiElementSubject(this)

internal class UsageInfoCollectionSubject(
    private val usageInfo: Collection<UsageInfo>
) : IterableSubject(
    Truth.THROW_ASSERTION_ERROR,
    usageInfo
) {
  fun hasElementAtCaret(file: String, caret: String): UsageInfoCollectionSubject {
    val position = SqlDelightFixtureTestCase.getPosition(file, caret)
    usageInfo.map { it.element }.filterNotNull().forEach {
      if (it.containingFile.name == File(file).name && it.textOffset == position) {
        return this
      }
    }
    throw AssertionError("Element at <$caret> in $file was not found as a usage")
  }
}

internal fun Collection<UsageInfo>.assertThat() = UsageInfoCollectionSubject(this)