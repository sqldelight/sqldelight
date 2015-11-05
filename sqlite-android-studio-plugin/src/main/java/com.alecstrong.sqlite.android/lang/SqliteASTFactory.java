package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.SQLiteLexer;
import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.psi.ASTWrapperPsiElement;
import com.alecstrong.sqlite.android.psi.IdentifierElement;
import com.alecstrong.sqlite.android.psi.ParseElement;
import com.alecstrong.sqlite.android.psi.TableNameElement;
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
  public interface PsiElementFactory {
    PsiElement createElement(ASTNode node);
  }

  private static final Map<IElementType, PsiElementFactory> ruleElementTypeToPsiFactory =
      new LinkedHashMap<IElementType, PsiElementFactory>();
  static {
    ruleElementTypeToPsiFactory.put(
        SqliteTokenTypes.RULE_ELEMENT_TYPES.get(SQLiteParser.RULE_parse),
        ParseElement.Factory.INSTANCE);
    ruleElementTypeToPsiFactory.put(
        SqliteTokenTypes.RULE_ELEMENT_TYPES.get(SQLiteParser.RULE_table_name),
        TableNameElement.Factory.INSTANCE);
  }

  @Nullable @Override public CompositeElement createComposite(IElementType type) {
    if (type instanceof IFileElementType) {
      return new FileElement(type, null);
    }
    return new CompositeElement(type);
  }

  @Nullable @Override
  public LeafElement createLeaf(@NotNull IElementType type, @NotNull CharSequence text) {
    if (type == SqliteTokenTypes.TOKEN_ELEMENT_TYPES.get(SQLiteLexer.IDENTIFIER)) {
      return new IdentifierElement(type, text);
    }
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
