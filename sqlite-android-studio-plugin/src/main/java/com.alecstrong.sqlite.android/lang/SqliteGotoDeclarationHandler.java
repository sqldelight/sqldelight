package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.SqliteCompiler;
import com.alecstrong.sqlite.android.generating.TableGenerator;
import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.SqlStmt;
import com.alecstrong.sqlite.android.psi.ColumnNameElement;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import static com.alecstrong.sqlite.android.lang.SqliteTokenTypes.RULE_ELEMENT_TYPES;

public class SqliteGotoDeclarationHandler implements GotoDeclarationHandler {
  @Nullable @Override
  public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset,
      Editor editor) {
    if (sourceElement == null
        || !(sourceElement.getParent() instanceof PsiReferenceExpressionImpl)) {
      return new PsiElement[0];
    }

    final PsiElement resolveElement =
        ((PsiReferenceExpressionImpl) sourceElement.getParent()).advancedResolve(true).getElement();
    if (resolveElement == null || !(resolveElement instanceof PsiField)) {
      return new PsiElement[0];
    }

    final ProjectRootManager projectManager =
        ProjectRootManager.getInstance(resolveElement.getProject());
    final PsiManager psiManager = PsiManager.getInstance(resolveElement.getProject());
    final VirtualFile elementFile = resolveElement.getContainingFile().getVirtualFile();
    final VirtualFile sourceRoot = projectManager.getFileIndex().getSourceRootForFile(elementFile);
    if (sourceRoot == null || !sourceRoot.getPath().endsWith(SqliteCompiler.OUTPUT_DIRECTORY)) {
      return new PsiElement[0];
    }

    PsiIdentifier identifier = null;
    for (PsiElement child : resolveElement.getChildren()) {
      if (child instanceof PsiIdentifier) {
        identifier = (PsiIdentifier) child;
        break;
      }
    }
    if (identifier == null) {
      return new PsiElement[0];
    }

    final PsiElement[] result = new PsiElement[1];
    final SqliteElementFilter filter = new SqliteElementFilter(identifier.getText());
    projectManager.getFileIndex().iterateContent(new SqliteContentIterator(psiManager) {
      @Override public boolean processFile(PsiFile file) {
        if (!SqliteCompiler.Companion.interfaceName(file.getVirtualFile().getNameWithoutExtension())
            .equals(elementFile.getNameWithoutExtension())) {
          // This is not the source sqlite file. Continue.
          return true;
        }

        PsiElement[] elements = PsiTreeUtil.collectElements(file, filter);
        result[0] = elements.length > 0 ? elements[0] : null;
        return false;
      }
    });

    return result[0] == null ? new PsiElement[0] : result;
  }

  @Nullable @Override public String getActionText(DataContext context) {
    return null;
  }

  private class SqliteElementFilter implements PsiElementFilter {

    private final String identifierText;

    SqliteElementFilter(String identifierText) {
      this.identifierText = identifierText;
    }

    @Override public boolean isAccepted(PsiElement element) {
      if (identifierText.equals(SqliteCompiler.TABLE_NAME)) {
        // If the identifier was for the table name, search for the create table name element.
        return element.getNode().getElementType() == RULE_ELEMENT_TYPES.get(
            SQLiteParser.RULE_create_table_stmt);
      }

      if (element instanceof ColumnNameElement) {
        // See if this is a matching column name.
        ColumnNameElement column = (ColumnNameElement) element;
        return column.getId().getName() != null
            && Column.Companion.fieldName(column.getId().getName()).equals(identifierText)
            && element.getParent().getNode().getElementType() == RULE_ELEMENT_TYPES.get(
            SQLiteParser.RULE_column_def);
      }

      if (element.getNode().getElementType() == RULE_ELEMENT_TYPES.get(
          SQLiteParser.RULE_sql_stmt)) {
        // See if this is a matching sqlite statemenet.
        ASTNode[] identifier =
            TableGenerator.childrenForTokens(element.getNode(), SQLiteParser.IDENTIFIER);
        return identifier.length > 0 && SqlStmt.Companion.fieldName(identifier[0].getText()).equals(
            identifierText);
      }

      return false;
    }
  }
}
