package com.squareup.sqlite.android.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile

class SqliteFile internal constructor(viewProvider: FileViewProvider)
: PsiFileBase(viewProvider, SqliteLanguage.INSTANCE) {
  var generatedFile: PsiFile? = null

  override fun getFileType() = SqliteFileType.INSTANCE
  override fun toString() = "SQLite file"
}
