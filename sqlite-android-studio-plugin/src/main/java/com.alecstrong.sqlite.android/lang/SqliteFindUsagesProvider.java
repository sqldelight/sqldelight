package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.psi.ColumnNameElement;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SqliteFindUsagesProvider implements FindUsagesProvider {
  @Nullable @Override public WordsScanner getWordsScanner() {
    return null;
  }

  @Override public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
    return psiElement instanceof ColumnNameElement;
  }

  @Nullable @Override public String getHelpId(@NotNull PsiElement psiElement) {
    return null;
  }

  @NotNull @Override public String getType(@NotNull PsiElement element) {
    if (element instanceof ColumnNameElement) {
      return "sqlite column";
    }

    return "";
  }

  @NotNull @Override public String getDescriptiveName(@NotNull PsiElement element) {
    return element.getText();
  }

  @NotNull @Override public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
    return element.getParent().getText();
  }
}
