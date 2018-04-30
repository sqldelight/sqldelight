package com.squareup.sqldelight.core.lang

import com.alecstrong.sqlite.psi.core.SqliteFileBase
import com.intellij.psi.FileViewProvider

class MigrationFile(
  viewProvider : FileViewProvider
) : SqliteFileBase(viewProvider, MigrationLanguage) {
  override fun getFileType() = MigrationFileType

}