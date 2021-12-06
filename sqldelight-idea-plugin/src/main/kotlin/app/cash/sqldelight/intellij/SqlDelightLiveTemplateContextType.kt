package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.lang.MigrationFileType
import app.cash.sqldelight.core.lang.SqlDelightFileType
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.psi.PsiFile

class SqlDelightLiveTemplateContextType : TemplateContextType("SQLDELIGHT", "SqlDelight") {

  private val supportedFileTypes = setOf(SqlDelightFileType, MigrationFileType)

  override fun isInContext(file: PsiFile, offset: Int): Boolean = file.fileType in supportedFileTypes
}
