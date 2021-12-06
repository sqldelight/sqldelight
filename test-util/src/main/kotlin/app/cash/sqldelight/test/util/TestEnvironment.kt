package app.cash.sqldelight.test.util

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightEnvironment
import app.cash.sqldelight.core.SqlDelightSourceFolder
import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import java.io.File

internal class TestEnvironment(
  private val outputDirectory: File = File("output"),
  private val deriveSchemaFromMigrations: Boolean = false,
  private val dialectPreset: DialectPreset = DialectPreset.SQLITE_3_18
) {
  fun build(
    root: String,
    annotationHolder: SqlAnnotationHolder
  ): SqlDelightEnvironment {
    val compilationUnit = object : SqlDelightCompilationUnit {
      override val name = "test"
      override val outputDirectoryFile = outputDirectory
      override val sourceFolders = emptyList<SqlDelightSourceFolder>()
    }
    val environment = SqlDelightEnvironment(
      sourceFolders = listOf(File(root)),
      dependencyFolders = emptyList(),
      properties = object : SqlDelightDatabaseProperties {
        override val packageName = "com.example"
        override val className = "TestDatabase"
        override val dependencies = emptyList<SqlDelightDatabaseName>()
        override val compilationUnits = listOf(compilationUnit)
        override val dialectPresetName = dialectPreset.name
        override val deriveSchemaFromMigrations = this@TestEnvironment.deriveSchemaFromMigrations
        override val rootDirectory = File(root)
      },
      verifyMigrations = true,
      // hyphen in the name tests that our module name sanitizing works correctly
      moduleName = "test-module",
      compilationUnit = compilationUnit,
    )
    environment.annotate(annotationHolder)
    return environment
  }
}
