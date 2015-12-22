package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.psi.ColumnNameElement;
import com.alecstrong.sqlite.android.util.SqliteRenameUtil;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.find.findUsages.JavaFindUsagesHandler;
import com.intellij.find.findUsages.JavaFindUsagesHandlerFactory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SqliteFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
  @Override public boolean canFindUsages(@NotNull PsiElement element) {
    return element instanceof ColumnNameElement;
  }

  @Nullable @Override
  public FindUsagesHandler createFindUsagesHandler(@NotNull final PsiElement element,
      boolean forHighlightUsages) {
    if (element instanceof ColumnNameElement) {
      return new JavaFindUsagesHandler(element,
          SqliteRenameUtil.getSecondaryElements((ColumnNameElement) element,
              (SqliteFile) element.getContainingFile()),
          JavaFindUsagesHandlerFactory.getInstance(element.getProject()));
    }

    return null;
  }
}
