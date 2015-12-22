package com.alecstrong.sqlite.android.psi;

import com.alecstrong.sqlite.android.SQLiteParser;
import com.alecstrong.sqlite.android.lang.SqliteFile;
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes;
import com.alecstrong.sqlite.android.util.SqliteRenameUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColumnNameElementRef extends SqliteElementRef {
  private TableNameElement leftTableDef;

  public ColumnNameElementRef(IdentifierElement idNode, String ruleName) {
    super(idNode, ruleName);
  }

  @Override public Class<? extends SqliteElement> identifierParentClass() {
    return ColumnNameElement.class;
  }

  @Override public IElementType identifierDefinitionRule() {
    return SqliteTokenTypes.RULE_ELEMENT_TYPES.get(SQLiteParser.RULE_column_def);
  }

  @NotNull @Override public Object[] getVariants() {
    leftTableDef = PsiTreeUtil.getPrevSiblingOfType(
        PsiTreeUtil.getParentOfType(getElement(), ColumnNameElement.class), TableNameElement.class);

    return super.getVariants();
  }

  @Nullable @Override public PsiElement resolve() {
    ColumnNameElement columnName =
        PsiTreeUtil.getParentOfType(getElement(), ColumnNameElement.class);
    if (columnName != null
        && columnName.getParent().getNode().getElementType() == identifierDefinitionRule()) {
      // If this is already a column definition return ourselves.
      return columnName;
    }
    leftTableDef = PsiTreeUtil.getPrevSiblingOfType(columnName, TableNameElement.class);

    return super.resolve();
  }

  @Override public PsiElement handleElementRename(final String newElementName) {
    SqliteFile file = (SqliteFile) myElement.getContainingFile();

    SqliteRenameUtil.SqliteUsageInfo usageInfo =
        SqliteRenameUtil.findUsages(myElement, newElementName, file);
    SqliteRenameUtil.doRename(myElement, newElementName, usageInfo, file, null);

    return myElement;
  }

  @Override public boolean isAccepted(PsiElement element) {
    if (leftTableDef == null) {
      // There was no table specified, all table names and column names are valid.
      return super.isAccepted(element) || (element instanceof TableNameElement
          && element.getParent().getNode().getElementType()
          == SqliteTokenTypes.RULE_ELEMENT_TYPES.get(SQLiteParser.RULE_create_table_stmt));
    } else {
      return super.isAccepted(element) && leftTableDef.isSameTable(
          PsiTreeUtil.getChildOfType(element.getParent().getParent(), TableNameElement.class));
    }
  }
}
