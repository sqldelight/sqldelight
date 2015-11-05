package com.alecstrong.sqlite.android.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTFactory;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqliteASTFactory extends ASTFactory {
  private static final Map<IElementType, PsiElementFactory> ruleElementTypeToPsiFactory =
      new LinkedHashMap<IElementType, PsiElementFactory>();

  static {
    // later auto gen with tokens from some spec in grammar?
    //ruleElementTypeToPsiFactory.put(
    //    ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_rules),
    //    RulesNode.Factory.INSTANCE);
  }

  @Nullable @Override public CompositeElement createComposite(IElementType type) {
    if (type instanceof IFileElementType) {
      return new FileElement(type, null);
    }
    return super.createComposite(type);
  }

  @Nullable @Override
  public LeafElement createLeaf(@NotNull IElementType type, @NotNull CharSequence text) {
    return new LeafPsiElement(type, text);
  }

  public static PsiElement createInternalParseTreeNode(ASTNode node) {
    PsiElementFactory factory = ruleElementTypeToPsiFactory.get(node.getElementType());
    if (factory != null) {
      return factory.createElement(node);
    }
    return new ASTWrapperPsiElement(node);
  }
}
