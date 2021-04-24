package com.squareup.sqldelight.core.lang

import com.alecstrong.sql.psi.core.SqlFileBase
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import com.squareup.sqldelight.core.SqlDelightFileIndex

class MigrationFile(
  viewProvider: FileViewProvider
) : SqlDelightFile(viewProvider, MigrationLanguage) {
  val version: Int by lazy {
    name.substringBeforeLast(".${fileType.EXTENSION}")
      .filter { it in '0'..'9' }.toIntOrNull() ?: 0
  }

  internal fun sqliteStatements() = sqlStmtList!!.stmtList

  override val packageName
    get() = module?.let { module -> SqlDelightFileIndex.getInstance(module).packageName }

  override val order
    get() = version

  override fun getFileType() = MigrationFileType

  override fun baseContributorFile(): SqlFileBase? {
    val module = module
    if (module == null || SqlDelightFileIndex.getInstance(
        module
      ).deriveSchemaFromMigrations
    ) return null

    val manager = PsiManager.getInstance(project)
    var result: SqlFileBase? = null
    val folders = SqlDelightFileIndex.getInstance(module).sourceFolders(virtualFile ?: return null)
    folders.forEach { dir ->
      VfsUtilCore.iterateChildrenRecursively(
        dir, { it.isDirectory || it.fileType == DatabaseFileType },
        { file ->
          if (file.isDirectory) return@iterateChildrenRecursively true

          val vFile = (manager.findViewProvider(file) as? DatabaseFileViewProvider)?.getSchemaFile()
            ?: return@iterateChildrenRecursively true

          manager.findFile(vFile)?.let { psiFile ->
            if (psiFile is SqlFileBase) {
              result = psiFile
              return@iterateChildrenRecursively false
            }
          }

          return@iterateChildrenRecursively true
        }
      )
    }
    return result
  }
}
