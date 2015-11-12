package com.alecstrong.sqlite.android.lang;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public class SqliteGotoDeclarationHandler implements GotoDeclarationHandler {
  @Nullable @Override
  public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset,
      Editor editor) {
    return new PsiElement[0];
  }

  @Nullable @Override public String getActionText(DataContext context) {
    return null;
  }
}
