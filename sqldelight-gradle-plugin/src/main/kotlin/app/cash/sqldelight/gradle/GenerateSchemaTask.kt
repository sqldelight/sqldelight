package app.cash.sqldelight.gradle

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightEnvironment
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.core.lang.util.forInitializationStatements
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.ServiceLoader
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

@CacheableTask
abstract class GenerateSchemaTask : SqlDelightWorkerTask() {
  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @get:Input abstract val projectName: Property<String>

  @get:Nested abstract var properties: SqlDelightDatabasePropertiesImpl

  @get:Nested abstract var compilationUnit: SqlDelightCompilationUnitImpl

  @get:Input abstract val verifyMigrations: Property<Boolean>

  @TaskAction
  fun generateSchemaFile() {
    workQueue().submit(GenerateSchema::class.java) {
      it.outputDirectory.set(outputDirectory)
      it.moduleName.set(projectName)
      it.properties.set(properties)
      it.verifyMigrations.set(verifyMigrations)
      it.compilationUnit.set(compilationUnit)
    }
  }

  @InputFiles
  @SkipWhenEmpty
  @IgnoreEmptyDirectories
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }

  interface GenerateSchemaWorkParameters : WorkParameters {
    val outputDirectory: DirectoryProperty
    val moduleName: Property<String>
    val properties: Property<SqlDelightDatabaseProperties>
    val compilationUnit: Property<SqlDelightCompilationUnit>
    val verifyMigrations: Property<Boolean>
  }

  abstract class GenerateSchema : WorkAction<GenerateSchemaWorkParameters> {

    private val sourceFolders: List<File>
      get() = parameters.compilationUnit.get().sourceFolders.map { it.folder }

    override fun execute() {
      val environment = SqlDelightEnvironment(
        sourceFolders = sourceFolders.filter { it.exists() },
        dependencyFolders = emptyList(),
        moduleName = parameters.moduleName.get(),
        properties = parameters.properties.get(),
        verifyMigrations = parameters.verifyMigrations.get(),
        compilationUnit = parameters.compilationUnit.get(),
        dialect = ServiceLoader.load(SqlDelightDialect::class.java).first(),
      )

      var maxVersion = 1L
      environment.forMigrationFiles { migrationFile ->
        maxVersion = maxOf(maxVersion, migrationFile.version + 1)
      }

      val outputDirectory = parameters.outputDirectory.get().asFile
      File("$outputDirectory/$maxVersion.db").apply {
        if (exists()) delete()
      }
      createConnection("$outputDirectory/$maxVersion.db").use { connection ->
        val sourceFiles = ArrayList<SqlDelightQueriesFile>()
        environment.forSourceFiles { file ->
          if (file is SqlDelightQueriesFile) sourceFiles.add(file)
        }
        sourceFiles.forInitializationStatements(
          environment.dialect.allowsReferenceCycles,
        ) { sqlText ->
          connection.prepareStatement(sqlText).execute()
        }
      }
    }

    private fun createConnection(path: String): Connection {
      return try {
        DriverManager.getConnection("jdbc:sqlite:$path")
      } catch (e: SQLException) {
        DriverManager.getConnection("jdbc:sqlite:$path")
      }
    }
  }
}
