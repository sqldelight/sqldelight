package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.lang.MigrationFileType
import app.cash.sqldelight.core.lang.SqlDelightFileType
import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType

class SqlDelightLiveTemplateContextType : TemplateContextType("SqlDelight") {

  private val supportedFileTypes = setOf(SqlDelightFileType, MigrationFileType)

  override fun isInContext(templateActionContext: TemplateActionContext): Boolean {
    return templateActionContext.file.fileType in supportedFileTypes
  }
}
