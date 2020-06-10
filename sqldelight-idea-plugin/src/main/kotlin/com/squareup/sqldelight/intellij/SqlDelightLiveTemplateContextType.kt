package com.squareup.sqldelight.intellij

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.psi.PsiFile
import com.squareup.sqldelight.core.lang.MigrationFileType
import com.squareup.sqldelight.core.lang.SqlDelightFileType

class SqlDelightLiveTemplateContextType : TemplateContextType("SQLDELIGHT", "SqlDelight") {

  private val supportedFileTypes = setOf(SqlDelightFileType, MigrationFileType)

  override fun isInContext(file: PsiFile, offset: Int): Boolean = file.fileType in supportedFileTypes
}
