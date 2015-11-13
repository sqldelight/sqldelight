package com.alecstrong.sqlite.android.psi;

import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClassElementRef extends PsiReferenceBase<ClassNameElement> {
  private String className;

  public ClassElementRef(ClassNameElement element,
      String className) {
    super(element, new TextRange(1, className.length() - 1));
    if (className.startsWith("\'")) {
      // Strip quotes.
      className = className.substring(1, className.length() - 1);
    }
    this.className = className;
  }

  @Nullable @Override public PsiElement resolve() {
    return JavaPsiFacade.getInstance(getElement().getProject())
        .findClass(className, ModuleUtilCore.findModuleForPsiElement(getElement())
            .getModuleWithDependenciesAndLibrariesScope(false));
  }

  @NotNull @Override public Object[] getVariants() {
    return EMPTY_ARRAY;
  }
}
