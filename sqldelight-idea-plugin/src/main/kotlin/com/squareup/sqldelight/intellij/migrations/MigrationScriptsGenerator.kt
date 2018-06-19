package com.squareup.sqldelight.intellij.migrations

import com.squareup.sqlite.migrations.DatabaseDiff

object MigrationScriptsGenerator {
  fun generateMigrationScripts(databaseDiff: DatabaseDiff): List<String> {
    // TODO
    return listOf(databaseDiff.toString())
  }
}
