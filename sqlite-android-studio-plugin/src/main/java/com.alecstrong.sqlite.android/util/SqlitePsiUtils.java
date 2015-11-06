package com.alecstrong.sqlite.android.util;

import com.alecstrong.sqlite.android.lang.SqliteLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;

@SuppressWarnings("SimplifiableIfStatement")
public class SqlitePsiUtils {
  public static PsiElement createLeafFromText(Project project, PsiElement context,
      String text, IElementType type) {
    PsiFileFactoryImpl factory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(project);
    PsiElement el = factory.createElementFromText(text,
        SqliteLanguage.INSTANCE,
        type,
        context);
    return PsiTreeUtil.getDeepestFirst(el);
  }
}