package com.squareup.sqldelight.core.lang

import com.intellij.psi.FileViewProvider
import com.squareup.sqldelight.core.SqlDelightFileIndex

class MigrationFile(
  viewProvider: FileViewProvider
) : SqlDelightFile(viewProvider, MigrationLanguage) {
  val version
    get() = virtualFile.nameWithoutExtension.filter { it in '0'..'9' }.toInt()

  internal fun sqliteStatements() = sqlStmtList!!.stmtList

  override val packageName = SqlDelightFileIndex.getInstance(module).packageName

  override val order = SqlDelightFileIndex.getInstance(module).ordering(this)

  override fun getFileType() = MigrationFileType
}
