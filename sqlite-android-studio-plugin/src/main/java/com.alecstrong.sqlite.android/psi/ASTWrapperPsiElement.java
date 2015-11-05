package com.alecstrong.sqlite.android.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ASTWrapperPsiElement extends com.intellij.extapi.psi.ASTWrapperPsiElement {
  public ASTWrapperPsiElement(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull @Override public PsiElement[] getChildren() {
    PsiElement psiChild = getFirstChild();
    if (psiChild == null) return PsiElement.EMPTY_ARRAY;

    List<PsiElement> result = new ArrayList<PsiElement>();
    while (psiChild != null) {
      result.add(psiChild);
      psiChild = psiChild.getNextSibling();
    }
    return PsiUtilCore.toPsiElementArray(result);
  }
}
