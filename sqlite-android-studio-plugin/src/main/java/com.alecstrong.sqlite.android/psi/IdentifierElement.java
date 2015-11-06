package com.alecstrong.sqlite.android.psi;

import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes;
import com.alecstrong.sqlite.android.util.SqlitePsiUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class IdentifierElement extends LeafPsiElement implements PsiNamedElement {
  private String name = null; // an override to input text ID if we rename via intellij

  public IdentifierElement(IElementType type, CharSequence text) {
    super(type, text);
  }

  @Override
  public String getName() {
    if (name != null) return name;
    return getText();
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    this.replace(SqlitePsiUtils.createLeafFromText(getProject(),
        getContext(),
        name, getRuleRefType()));
    this.name = name;
    return this;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + getElementType().toString() + ")";
  }

  private IElementType getRuleRefType() {
    return SqliteTokenTypes.TOKEN_ELEMENT_TYPES.get(SQLiteParser.IDENTIFIER);
  }

  @Override public PsiReference getReference() {
    if (PsiTreeUtil.getParentOfType(this, TableNameElement.class) != null) {
      return new TableNameElementRef(this, getText());
    } else if (PsiTreeUtil.getParentOfType(this, ColumnNameElement.class) != null) {
      return new ColumnNameElementRef(this, getText());
    }
    return null;
  }
}
