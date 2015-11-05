package com.alecstrong.sqlite.android.psi;

import com.alecstrong.sqlite.android.lang.SqliteASTFactory;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class ParseElement extends ASTWrapperPsiElement {
  public ParseElement(@NotNull ASTNode node) {
    super(node);
  }

  public static final class Factory implements SqliteASTFactory.PsiElementFactory {
    public static final Factory INSTANCE = new Factory();

    @Override public PsiElement createElement(ASTNode node) {
      return new ParseElement(node);
    }
  }
}
