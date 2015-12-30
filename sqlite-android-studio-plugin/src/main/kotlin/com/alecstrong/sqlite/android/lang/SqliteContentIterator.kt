package com.alecstrong.sqlite.android.lang

import com.alecstrong.sqlite.android.SqliteCompiler
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class SqliteContentIterator(private val psiManager: PsiManager,
    private val processor: (file: PsiFile) -> Boolean) : ContentIterator {
  override fun processFile(fileOrDir: VirtualFile): Boolean {
    return fileOrDir.isDirectory || fileOrDir.extension != SqliteCompiler.FILE_EXTENSION ||
        processor(psiManager.findFile(fileOrDir) ?: return true)
  }
}
