package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.SqliteCompiler;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public abstract class SqliteContentIterator implements ContentIterator {
  private final PsiManager psiManager;

  protected SqliteContentIterator(PsiManager psiManager) {
    this.psiManager = psiManager;
  }

  @Override public boolean processFile(VirtualFile fileOrDir) {
    if (fileOrDir.isDirectory()) return true;
    if (!fileOrDir.getExtension().equals(SqliteCompiler.getFileExtension())) return true;
    return processFile(psiManager.findFile(fileOrDir));
  }

  public abstract boolean processFile(PsiFile file);
}
