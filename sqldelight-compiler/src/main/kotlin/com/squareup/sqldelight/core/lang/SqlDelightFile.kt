package com.squareup.sqldelight.core.lang

import com.alecstrong.sql.psi.core.SqlFileBase
import com.intellij.lang.Language
import com.intellij.openapi.module.Module
import com.intellij.psi.FileViewProvider
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightProjectService

abstract class SqlDelightFile(
  viewProvider: FileViewProvider,
  language: Language
) : SqlFileBase(viewProvider, language) {
  protected val module: Module
    get() = SqlDelightProjectService.getInstance(project).module(requireNotNull(virtualFile, { "Null virtualFile" }))!!

  val generatedDir by lazy {
    "${SqlDelightFileIndex.getInstance(module).outputDirectory}/${packageName.replace('.', '/')}"
  }

  internal val dialect
    get() = SqlDelightProjectService.getInstance(project).dialectPreset

  internal abstract val packageName: String
}
