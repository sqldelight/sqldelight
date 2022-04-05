package app.cash.sqldelight.test.util

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightEnvironment
import app.cash.sqldelight.core.SqlDelightSourceFolder
import app.cash.sqldelight.core.lang.MigrationLanguage
import app.cash.sqldelight.core.lang.SqlDelightLanguage
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialects.sqlite_3_18.SqliteDialect
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.intellij.lang.LanguageParserDefinitions
import java.io.File

internal class TestEnvironment(
  private val outputDirectory: File = File("output"),
  private val deriveSchemaFromMigrations: Boolean = false,
  private val treatNullAsUnknownForEquality: Boolean = false,
  private val dialect: SqlDelightDialect = SqliteDialect()
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
        override val deriveSchemaFromMigrations = this@TestEnvironment.deriveSchemaFromMigrations
        override val treatNullAsUnknownForEquality = this@TestEnvironment.treatNullAsUnknownForEquality
        override val rootDirectory = File(root)
      },
      dialect = dialect,
      verifyMigrations = true,
      // hyphen in the name tests that our module name sanitizing works correctly
      moduleName = "test-module",
      compilationUnit = compilationUnit,
    )
    LanguageParserDefinitions.INSTANCE.forLanguage(SqlDelightLanguage).createParser(environment.project)
    LanguageParserDefinitions.INSTANCE.forLanguage(MigrationLanguage).createParser(environment.project)
    environment.annotate(annotationHolder)
    return environment
  }
}
