package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.core.lang.MigrationLanguage
import app.cash.sqldelight.core.lang.SqlDelightLanguage
import com.intellij.ide.FileIconProvider
import com.intellij.lang.Language
import com.intellij.lang.LanguageUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class SqlDelightFileIconProvider : FileIconProvider {

  override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? =
    getIcon(LanguageUtil.getFileLanguage(file), project)

  companion object {
    private val supportedLanguages = setOf(SqlDelightLanguage, MigrationLanguage)

    fun getIcon(language: Language?, project: Project?): Icon? {
      return if (project != null && language in supportedLanguages) {
        SqlDelightProjectService.getInstance(project).dialect.icon
      } else {
        null
      }
    }
  }
}
