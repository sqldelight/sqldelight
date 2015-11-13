package com.alecstrong.sqlite.android.psi;

import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class ClassNameElement extends LeafPsiElement {
  private final Condition<PsiElement> isClassString = new Condition<PsiElement>() {
    @Override public boolean value(PsiElement psiElement) {
      return psiElement instanceof ASTWrapperPsiElement
          && psiElement.getNode().getElementType() == SqliteTokenTypes.RULE_ELEMENT_TYPES.get(
          SQLiteParser.RULE_sqlite_class_name);
    }
  };

  public ClassNameElement(@NotNull IElementType type, CharSequence text) {
    super(type, text);
  }

  @Override public PsiReference getReference() {
    if (PsiTreeUtil.findFirstParent(this, isClassString) != null) {
      return new ClassNameElementRef(this, getText());
    }
    return null;
  }
}
