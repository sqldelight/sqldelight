package app.cash.sqldelight.core.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType

const val MIGRATION_EXTENSION = "sqm"

object MigrationFileType : LanguageFileType(MigrationLanguage) {
  private val ICON = AllIcons.Providers.Sqlite

  override fun getName() = "SqlDelight Migration"
  override fun getDescription() = "SqlDelight Migration"
  override fun getDefaultExtension() = MIGRATION_EXTENSION
  override fun getIcon() = ICON
}
