package com.alecstrong.sqlite.android.psi;

import com.alecstrong.sqlite.android.SQLiteLexer;
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes;
import com.alecstrong.sqlite.android.util.SqlitePsiUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SqliteElementRef extends PsiReferenceBase<IdentifierElement> {
  String ruleName;

  public SqliteElementRef(IdentifierElement idNode, String ruleName) {
    super(idNode, new TextRange(0, ruleName.length()));
    this.ruleName = ruleName;
  }

  public abstract Class<? extends SqliteElement> identifierParentClass();

  public abstract IElementType identifierDefinitionRule();

  @NotNull
  @Override
  public Object[] getVariants() {
    ParseElement rules = PsiTreeUtil.getContextOfType(myElement, ParseElement.class);
    Collection<? extends SqliteElement> ruleSpecNodes =
        PsiTreeUtil.findChildrenOfType(rules, identifierParentClass());

    return ruleSpecNodes.toArray();
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    return SqlitePsiUtils.findRuleSpecNodeAbove(getElement(), ruleName, this);
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    Project project = getElement().getProject();
    myElement.replace(SqlitePsiUtils.createLeafFromText(project,
        myElement.getContext(),
        newElementName,
        SqliteTokenTypes.TOKEN_ELEMENT_TYPES.get(SQLiteLexer.IDENTIFIER)));
    return myElement;
  }
}
