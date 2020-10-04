package com.squareup.sqldelight.core.lang

import com.intellij.psi.FileViewProvider
import com.squareup.sqldelight.core.SqlDelightFileIndex

class MigrationFile(
  viewProvider: FileViewProvider
) : SqlDelightFile(viewProvider, MigrationLanguage) {
  val version: Int by lazy {
    name.substringBeforeLast(".${fileType.EXTENSION}")
        .filter { it in '0'..'9' }.toInt()
  }

  internal fun sqliteStatements() = sqlStmtList!!.stmtList

  override val packageName
    get() = module?.let { module -> SqlDelightFileIndex.getInstance(module).packageName }

  override val order
    get() = version

  override fun getFileType() = MigrationFileType
}
