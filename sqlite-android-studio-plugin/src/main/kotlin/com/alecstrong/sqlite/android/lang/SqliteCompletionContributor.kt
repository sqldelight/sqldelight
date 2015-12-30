package com.alecstrong.sqlite.android.lang

import com.alecstrong.sqlite.android.psi.ClassNameElement
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil.findReferenceOrAlphanumericPrefix
import com.intellij.codeInsight.completion.JavaClassNameCompletionContributor

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
