package com.alecstrong.sqlite.android.psi;

import com.alecstrong.sqlite.android.util.SqlitePsiUtils;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public abstract class SqliteElement extends ASTWrapperPsiElement implements PsiNamedElement {
  protected String name = null; // an override to input text ID

  public SqliteElement(@NotNull final ASTNode node) {
    super(node);
  }

  @Override
  public String getName() {
    if ( name!=null ) return name;
    IdentifierElement id = getId();
    if ( id!=null ) {
      return id.getText();
    }
    return "unknown-name";
  }

  public abstract IdentifierElement getId();

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
		/*
		From doc: "Creating a fully correct AST node from scratch is
		          quite difficult. Thus, surprisingly, the easiest way to
		          get the replacement node is to create a dummy file in the
		          custom language so that it would contain the necessary
		          node in its parse tree, build the parse tree and
		          extract the necessary node from it.
		 */
    //		System.out.println("rename "+this+" to "+name);
    IdentifierElement id = getId();
    id.replace(SqlitePsiUtils.createLeafFromText(getProject(),
        getContext(),
        name, getRuleRefType()));
    this.name = name;
    return this;
  }

  public abstract IElementType getRuleRefType();

  @Override
  public void subtreeChanged() {
    super.subtreeChanged();
    name = null;
  }

  @Override
  public int getTextOffset() {
    IdentifierElement id = getId();
    if ( id!=null ) return id.getTextOffset();
    return super.getTextOffset();
  }
}
