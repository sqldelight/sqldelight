package com.alecstrong.sqlite.android.psi;

import com.alecstrong.sqlite.android.SQLiteLexer;
import com.alecstrong.sqlite.android.lang.SqliteContentIterator;
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes;
import com.alecstrong.sqlite.android.util.SqlitePsiUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SqliteElementRef extends PsiReferenceBase<IdentifierElement>
    implements PsiElementFilter {
  protected final PsiManager psiManager = PsiManager.getInstance(getElement().getProject());
  protected final ProjectRootManager projectRootManager =
      ProjectRootManager.getInstance(getElement().getProject());

  private String ruleName;

  public SqliteElementRef(IdentifierElement idNode, String ruleName) {
    super(idNode, new TextRange(0, ruleName.length()));
    this.ruleName = ruleName;
  }

  public abstract Class<? extends SqliteElement> identifierParentClass();

  public abstract IElementType identifierDefinitionRule();

  @NotNull
  @Override
  public Object[] getVariants() {
    final List<PsiElement> ruleSpecNodes = new ArrayList<PsiElement>();
    projectRootManager.getFileIndex().iterateContent(new SqliteContentIterator(psiManager) {
      @Override public boolean processFile(PsiFile file) {
        ruleSpecNodes.addAll(
            Arrays.asList(PsiTreeUtil.collectElements(file, SqliteElementRef.this)));
        return true;
      }
    });

    return ruleSpecNodes.toArray();
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    final PsiElement[] result = new PsiElement[] { null };
    projectRootManager.getFileIndex().iterateContent(new SqliteContentIterator(psiManager) {
      @Override public boolean processFile(PsiFile file) {
        for (PsiElement accepted : PsiTreeUtil.collectElements(file, SqliteElementRef.this)) {
          PsiElement nameNode = accepted.getFirstChild();
          if (nameNode != null && nameNode.getText().equals(ruleName)) {
            result[0] = accepted;
          }
        }
        return result[0] == null;
      }
    });
    return result[0];
  }

  @Override public boolean isAccepted(PsiElement element) {
    return identifierParentClass().isInstance(element)
        && element.getParent().getNode().getElementType() == identifierDefinitionRule();
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
