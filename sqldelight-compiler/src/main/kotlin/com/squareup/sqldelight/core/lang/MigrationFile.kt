package com.squareup.sqldelight.core.lang

import com.alecstrong.sql.psi.core.SqlFileBase
import com.intellij.psi.FileViewProvider

class MigrationFile(
  viewProvider : FileViewProvider
) : SqlFileBase(viewProvider, MigrationLanguage) {
  val version
    get() = virtualFile.nameWithoutExtension.toInt()

  internal fun sqliteStatements() = sqlStmtList!!.stmtList

  override fun getFileType() = MigrationFileType
}