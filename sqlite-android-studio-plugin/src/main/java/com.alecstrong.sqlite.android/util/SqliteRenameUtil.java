package com.alecstrong.sqlite.android.util;

import com.alecstrong.sqlite.android.lang.SqliteFile;
import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.psi.ColumnNameElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.impl.source.PsiFieldImpl;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.usageView.UsageInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SqliteRenameUtil {
  /**
   * Gather usage info for the given named element, where {@link PsiNamedElement#getName()} is
   * a valid column name used for generating Java code. This returns a {@link SqliteUsageInfo}
   * containing sqlite usages, field usages and method usages (which could be the interface or
   * marshal method).
   * @param originatingFile the file containing the given element.
   */
  public static SqliteUsageInfo findUsages(final PsiNamedElement element,
      final String newElementName, final SqliteFile originatingFile) {
    UsageInfo[] sqliteUsages = RenameUtil.findUsages(element, newElementName, false, false,
        Collections.<ColumnNameElement, String>emptyMap());

    final List<UsageInfo> fieldUsages = new ArrayList<UsageInfo>();
    final List<UsageInfo> methodUsages = new ArrayList<UsageInfo>();

    PsiTreeUtil.processElements(originatingFile.getGeneratedFile(), new PsiElementProcessor() {
      @Override public boolean execute(@NotNull PsiElement candidate) {
        if (candidate instanceof PsiFieldImpl && ((PsiFieldImpl) candidate).getName()
            .equals(Column.fieldName(element.getName()))) {
          fieldUsages.addAll(notInsideFile(
              RenameUtil.findUsages(candidate, Column.fieldName(newElementName), false, false,
                  Collections.<PsiElement, String>emptyMap()), originatingFile.getGeneratedFile()));
        } else if (candidate instanceof PsiMethodImpl && ((PsiMethodImpl) candidate).getName()
            .equals(Column.methodName(element.getName()))) {
          methodUsages.addAll(notInsideFile(
              RenameUtil.findUsages(candidate, Column.methodName(newElementName), false, false,
                  Collections.<PsiElement, String>emptyMap()), originatingFile.getGeneratedFile()));
        }
        return true;
      }
    });
    return new SqliteUsageInfo(fieldUsages.toArray(new UsageInfo[fieldUsages.size()]),
        methodUsages.toArray(new UsageInfo[methodUsages.size()]), sqliteUsages);
  }

  /**
   * Rename the given element by using the {@link SqliteUsageInfo} provided. It performs three
   * separate rename batches: field usages, method usages and sqlite usages. This function should
   * be called from a single command, so that undo functions properly.
   */
  public static void doRename(PsiElement element, String newElementName, SqliteUsageInfo usageInfo,
      SqliteFile originatingFile, @Nullable RefactoringElementListener listener) {
    for (UsageInfo fieldUsage : usageInfo.fieldUsages) {
      RenameUtil.rename(fieldUsage, Column.fieldName(newElementName));
    }
    for (UsageInfo methodUsage : usageInfo.methodUsages) {
      RenameUtil.rename(methodUsage, Column.methodName(newElementName));
    }
    RenameUtil.doRename(element, newElementName, usageInfo.sqliteUsages,
        originatingFile.getProject(), listener);
  }

  /**
   * see {@link #findUsages(PsiNamedElement, String, SqliteFile)}
   */
  private static Collection<UsageInfo> notInsideFile(UsageInfo[] original, PsiFile file) {
    List<UsageInfo> result = new ArrayList<UsageInfo>();
    for (UsageInfo usageInfo : original) {
      if (usageInfo.getFile() == file) continue;
      result.add(usageInfo);
    }
    return result;
  }

  public static final class SqliteUsageInfo {
    private final UsageInfo[] fieldUsages;
    private final UsageInfo[] methodUsages;
    private final UsageInfo[] sqliteUsages;

    public SqliteUsageInfo(UsageInfo[] fieldUsages, UsageInfo[] methodUsages,
        UsageInfo[] sqliteUsages) {
      this.fieldUsages = fieldUsages;
      this.methodUsages = methodUsages;
      this.sqliteUsages = sqliteUsages;
    }
  }
}
