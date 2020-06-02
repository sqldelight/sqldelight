package com.squareup.sqldelight.intellij

import com.alecstrong.sql.psi.core.DialectPreset
import com.intellij.icons.AllIcons
import com.intellij.ide.FileIconProvider
import com.intellij.lang.LanguageUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.squareup.sqldelight.core.SqlDelightProjectService
import com.squareup.sqldelight.core.lang.MigrationLanguage
import com.squareup.sqldelight.core.lang.SqlDelightLanguage
import javax.swing.Icon

class SqlDelightFileIconProvider : FileIconProvider {

  private val supportedLanguages = setOf(SqlDelightLanguage, MigrationLanguage)

  override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
    return if (project != null && LanguageUtil.getFileLanguage(file) in supportedLanguages) {
      when (SqlDelightProjectService.getInstance(project).dialectPreset) {
        DialectPreset.SQLITE_3_18 -> AllIcons.Providers.Sqlite
        DialectPreset.SQLITE_3_24 -> AllIcons.Providers.Sqlite
        DialectPreset.MYSQL -> AllIcons.Providers.Mysql
        DialectPreset.POSTGRESQL -> AllIcons.Providers.Postgresql
        DialectPreset.HSQL -> AllIcons.Providers.Hsqldb
      }
    } else {
      null
    }
  }
}
