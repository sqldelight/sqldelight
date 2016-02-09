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
package com.squareup.sqlite.android.lang

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil.findReferenceOrAlphanumericPrefix
import com.intellij.codeInsight.completion.JavaClassNameCompletionContributor
import com.squareup.sqlite.android.psi.ClassNameElement

class SqliteCompletionContributor : JavaClassNameCompletionContributor() {
  override fun fillCompletionVariants(parameters: CompletionParameters,
      resultSet: CompletionResultSet) {
    if (parameters.position is ClassNameElement) {
      val result = resultSet.withPrefixMatcher(findReferenceOrAlphanumericPrefix(parameters))
      JavaClassNameCompletionContributor.addAllClasses(parameters, parameters.invocationCount <= 1,
          result.prefixMatcher, resultSet)
    }
  }
}
