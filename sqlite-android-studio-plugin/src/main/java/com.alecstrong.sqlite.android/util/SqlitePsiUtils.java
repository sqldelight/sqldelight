package com.alecstrong.sqlite.android.util;

import com.alecstrong.sqlite.android.lang.SqliteLanguage;
import com.alecstrong.sqlite.android.psi.IdentifierElement;
import com.alecstrong.sqlite.android.psi.ParseElement;
import com.alecstrong.sqlite.android.psi.SqliteElementRef;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("SimplifiableIfStatement")
public class SqlitePsiUtils {

  @Nullable
  public static PsiElement findFirstChildOfType(final PsiElement parent, IElementType type) {
    return findFirstChildOfType(parent, TokenSet.create(type));
  }

  /**
   * traverses the psi tree depth-first, returning the first it finds with the given types
   *
   * @param parent the element whose children will be searched
   * @param types the types to search for
   * @return the first child, or null;
   */
  @Nullable
  public static PsiElement findFirstChildOfType(final PsiElement parent, final TokenSet types) {
    Iterator<PsiElement> iterator = findChildrenOfType(parent, types).iterator();
    if (iterator.hasNext()) return iterator.next();
    return null;
  }

  public static Iterable<PsiElement> findChildrenOfType(final PsiElement parent,
      IElementType type) {
    return findChildrenOfType(parent, TokenSet.create(type));
  }

  /**
   * Like PsiTreeUtil.findChildrenOfType, except no collection is created and it doesnt use
   * recursion.
   *
   * @param parent the element whose children will be searched
   * @param types the types to search for
   * @return an iterable that will traverse the psi tree depth-first, including only the elements
   * whose type is contained in the provided tokenset.
   */
  public static Iterable<PsiElement> findChildrenOfType(final PsiElement parent,
      final TokenSet types) {
    return new Iterable<PsiElement>() {
      @NotNull
      @Override
      public Iterator<PsiElement> iterator() {
        return Iterators.filter(new DepthFirstPsiIterator(parent), includeElementTypes(types));
      }
    };
  }

  static Predicate<PsiElement> includeElementTypes(final TokenSet tokenSet) {
    return new Predicate<PsiElement>() {

      @Override
      public boolean apply(@Nullable PsiElement input) {
        if (input == null) return false;
        ASTNode node = input.getNode();
        if (node == null) return false;
        return tokenSet.contains(node.getElementType());
      }
    };
  }

  static class DepthFirstPsiIterator extends AbstractIterator<PsiElement> {

    final PsiElement startFrom;

    DepthFirstPsiIterator(PsiElement startFrom) {
      this.startFrom = this.element = startFrom;
    }

    PsiElement element;

    private boolean tryNext(PsiElement candidate) {
      if (candidate != null) {
        element = candidate;
        return true;
      } else {
        return false;
      }
    }

    private boolean upAndOver(PsiElement parent) {
      while (parent != null && !parent.equals(startFrom)) {
        if (tryNext(parent.getNextSibling())) {
          return true;
        } else {
          parent = parent.getParent();
        }
      }
      return false;
    }

    @Override
    protected PsiElement computeNext() {
      if (tryNext(element.getFirstChild()) ||
          tryNext(element.getNextSibling()) ||
          upAndOver(element.getParent())) {
        return element;
      }
      return endOfData();
    }
  }

  public static PsiElement findRuleSpecNodeAbove(IdentifierElement element,
      final String ruleName, SqliteElementRef elementReference) {
    ParseElement sql = PsiTreeUtil.getContextOfType(element, ParseElement.class);
    return findRuleSpecNode(ruleName, sql, elementReference);
  }

  public static PsiElement findRuleSpecNode(final String ruleName, ParseElement sql,
      final SqliteElementRef elementReference) {
    PsiElementFilter defnode = new PsiElementFilter() {
      @Override
      public boolean isAccepted(PsiElement element) {
        PsiElement nameNode = element.getFirstChild();
        if (nameNode == null) return false;
        return (elementReference.identifierParentClass().isInstance(element)) &&
            element.getParent().getNode().getElementType() == elementReference.identifierDefinitionRule() &&
            nameNode.getText().equals(ruleName);
      }
    };
    PsiElement[] ruleSpec = PsiTreeUtil.collectElements(sql, defnode);
    if (ruleSpec.length > 0) return ruleSpec[0];
    return null;
  }

  public static PsiElement createLeafFromText(Project project, PsiElement context,
      String text, IElementType type) {
    PsiFileFactoryImpl factory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(project);
    PsiElement el = factory.createElementFromText(text,
        SqliteLanguage.INSTANCE,
        type,
        context);
    return PsiTreeUtil.getDeepestFirst(el); // forces parsing of file!!
    // start rule depends on root passed in
  }

  public static void replacePsiFileFromText(final Project project, final PsiFile psiFile,
      String text) {
    final PsiFile newPsiFile = createFile(project, text);
    WriteCommandAction setTextAction = new WriteCommandAction(project) {
      @Override
      protected void run(final Result result) throws Throwable {
        psiFile.deleteChildRange(psiFile.getFirstChild(), psiFile.getLastChild());
        psiFile.addRange(newPsiFile.getFirstChild(), newPsiFile.getLastChild());
      }
    };
    setTextAction.execute();
  }

  public static PsiFile createFile(Project project, String text) {
    String fileName = "a.g4"; // random name but must be .g4
    PsiFileFactoryImpl factory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(project);
    return factory.createFileFromText(fileName, SqliteLanguage.INSTANCE,
        text, false, false);
  }

  /**
   * Search all internal and leaf nodes looking for token or internal node with specific text. This
   * saves having to create lots of java classes just to identify psi nodes.
   */
  public static PsiElement[] collectNodesWithName(PsiElement root, final String tokenText) {
    return PsiTreeUtil.collectElements(root, new PsiElementFilter() {
      @Override
      public boolean isAccepted(PsiElement element) {
        String tokenTypeName = element.getNode().getElementType().toString();
        return tokenTypeName.equals(tokenText);
      }
    });
  }

  public static PsiElement[] collectNodesWithText(PsiElement root, final String text) {
    return PsiTreeUtil.collectElements(root, new PsiElementFilter() {
      @Override
      public boolean isAccepted(PsiElement element) {
        return element.getText().equals(text);
      }
    });
  }

  public static PsiElement[] collectChildrenOfType(PsiElement root, final IElementType tokenType) {
    List<PsiElement> elems = new ArrayList<PsiElement>();
    for (PsiElement child : root.getChildren()) {
      if (child.getNode().getElementType() == tokenType) {
        elems.add(child);
      }
    }
    return elems.toArray(new PsiElement[elems.size()]);
  }

  public static PsiElement findChildOfType(PsiElement root, final IElementType tokenType) {
    List<PsiElement> elems = new ArrayList<PsiElement>();
    for (PsiElement child : root.getChildren()) {
      if (child.getNode().getElementType() == tokenType) {
        return child;
      }
    }
    return null;
  }

  public static PsiElement[] collectChildrenWithText(PsiElement root, final String text) {
    List<PsiElement> elems = new ArrayList<PsiElement>();
    for (PsiElement child : root.getChildren()) {
      if (child.getText().equals(text)) {
        elems.add(child);
      }
    }
    return elems.toArray(new PsiElement[elems.size()]);
  }
}