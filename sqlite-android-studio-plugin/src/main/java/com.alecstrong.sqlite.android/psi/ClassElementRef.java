package com.alecstrong.sqlite.android.psi;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClassElementRef extends PsiReferenceBase<StringLiteralElement> {
  private String className;

  public ClassElementRef(StringLiteralElement element,
      String className) {
    super(element, new TextRange(0, className.length()));
    this.className = className;
  }

  @Nullable @Override public PsiElement resolve() {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(getElement().getProject());
    final Module module = ModuleUtilCore.findModuleForPsiElement(getElement());

    return facade.findClass(className, module.getModuleWithDependenciesAndLibrariesScope(false));
  }

  @NotNull @Override public Object[] getVariants() {
    return new Object[0];
  }
}
