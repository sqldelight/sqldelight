package app.cash.sqldelight.intellij.util

import app.cash.sqldelight.core.SqlDelightProjectService
import com.alecstrong.sql.psi.core.DialectPreset
import com.intellij.openapi.project.Project

internal val Project.dialectPreset: DialectPreset
  get() = SqlDelightProjectService.getInstance(this).dialectPreset

internal val DialectPreset.isSqlite: Boolean
  get() = name.startsWith("sqlite", true)
