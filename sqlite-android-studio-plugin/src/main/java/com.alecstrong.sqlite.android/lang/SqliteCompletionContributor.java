package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.psi.ClassNameElement;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.completion.JavaClassNameCompletionContributor;
import org.jetbrains.annotations.NotNull;

public class SqliteCompletionContributor extends JavaClassNameCompletionContributor {
  @Override public void fillCompletionVariants(@NotNull CompletionParameters parameters,
      @NotNull CompletionResultSet _result) {
    if (parameters.getPosition() instanceof ClassNameElement) {
      CompletionResultSet result = _result.withPrefixMatcher(
          CompletionUtil.findReferenceOrAlphanumericPrefix(parameters));
      addAllClasses(parameters, parameters.getInvocationCount() <= 1, result.getPrefixMatcher(),
          _result);
    }
  }
}
