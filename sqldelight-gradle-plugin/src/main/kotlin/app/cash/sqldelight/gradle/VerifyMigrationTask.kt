package app.cash.sqldelight.gradle

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightEnvironment
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.core.lang.util.forInitializationStatements
import app.cash.sqldelight.core.lang.util.rawSqlText
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqlite.migrations.CatalogDatabase
import app.cash.sqlite.migrations.ObjectDifferDatabaseComparator
import app.cash.sqlite.migrations.findDatabaseFiles
import java.io.File
import java.sql.DriverManager
import java.util.Properties
import java.util.ServiceLoader
import kotlin.collections.ArrayList
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

@CacheableTask
abstract class VerifyMigrationTask : SqlDelightWorkerTask() {
  @get:Input abstract val projectName: Property<String>

  /** Directory where the database files are copied for the migration scripts to run against. */
  @get:Internal abstract val workingDirectory: DirectoryProperty

  @get:Nested abstract var properties: SqlDelightDatabasePropertiesImpl

  @get:Nested abstract var compilationUnit: SqlDelightCompilationUnitImpl

  @get:Input abstract val verifyMigrations: Property<Boolean>

  @get:Input abstract val verifyDefinitions: Property<Boolean>

  @get:Input abstract val driverProperties: MapProperty<String, String>

  /* Tasks without an output are never considered UP-TO-DATE by Gradle. Adding an output file that's created when the
   * task completes successfully works around the lack of an output for this task. There may be a better solution once
   * https://github.com/gradle/gradle/issues/14223 is resolved. */
  @OutputFile
  internal fun getDummyOutputFile(): File = File(temporaryDir, "success.txt")

  @TaskAction
  fun verifyMigrations() {
    val workQueue = workQueue()
    workQueue.submit(VerifyMigrationAction::class.java) {
      it.workingDirectory.set(workingDirectory)
      it.projectName.set(projectName)
      it.properties.set(properties)
      it.verifyMigrations.set(verifyMigrations)
      it.compilationUnit.set(compilationUnit)
      it.verifyDefinitions.set(verifyDefinitions)
      it.driverProperties.set(driverProperties)
      it.outputFile.set(getDummyOutputFile())
    }
  }

  @InputFiles
  @SkipWhenEmpty
  @IgnoreEmptyDirectories
  @PathSensitive(PathSensitivity.RELATIVE)
  override fun getSource(): FileTree {
    return super.getSource()
  }

  interface VerifyMigrationWorkParameters : WorkParameters {
    val workingDirectory: DirectoryProperty
    val projectName: Property<String>
    val properties: Property<SqlDelightDatabaseProperties>
    val compilationUnit: Property<SqlDelightCompilationUnit>
    val verifyMigrations: Property<Boolean>
    val verifyDefinitions: Property<Boolean>
    val driverProperties: MapProperty<String, String>
    val outputFile: RegularFileProperty
  }

  abstract class VerifyMigrationAction : WorkAction<VerifyMigrationWorkParameters> {
    private val logger = Logging.getLogger(VerifyMigrationTask::class.java)

    private val sourceFolders: List<File>
      get() = parameters.compilationUnit.get().sourceFolders.map { it.folder }

    private val environment by lazy {
      SqlDelightEnvironment(
        sourceFolders = sourceFolders.filter { it.exists() },
        dependencyFolders = emptyList(),
        moduleName = parameters.projectName.get(),
        properties = parameters.properties.get(),
        verifyMigrations = parameters.verifyMigrations.get(),
        compilationUnit = parameters.compilationUnit.get(),
        dialect = ServiceLoader.load(SqlDelightDialect::class.java).first(),
      )
    }

    override fun execute() {
      ServiceLoader.load(DriverInitializer::class.java).firstOrNull()?.execute(
        parameters.properties.get(),
        parameters.driverProperties.toProperties(),
      )
      if (!environment.dialect.isSqlite) return
      parameters.workingDirectory.get().asFile.deleteRecursively()
      val catalog = createCurrentDb()

      val databaseFiles = sourceFolders.asSequence()
        .findDatabaseFiles()

      check(!parameters.verifyMigrations.get() || databaseFiles.count() > 0) {
        "Verifying a migration requires a database file to be present. To generate one, use the generate schema Gradle task for your database. A quick way to find the task name(s) is to run `gradle :module:tasks | grep generate`."
      }

      databaseFiles.forEach { dbFile ->
        checkMigration(dbFile, catalog)
      }

      checkForGaps()
      parameters.outputFile.get().asFile.createNewFile()
    }

    private fun createCurrentDb(): CatalogDatabase {
      val sourceFiles = ArrayList<SqlDelightQueriesFile>()
      environment.forSourceFiles { file ->
        if (file is SqlDelightQueriesFile) sourceFiles.add(file)
      }
      val initStatements = ArrayList<CatalogDatabase.InitStatement>()
      sourceFiles.forInitializationStatements(
        environment.dialect.allowsReferenceCycles,
      ) { sqlText ->
        initStatements.add(CatalogDatabase.InitStatement(sqlText, "Error compiling $sqlText"))
      }
      return CatalogDatabase.withInitStatements(initStatements)
    }

    private fun checkMigration(
      dbFile: File,
      currentDb: CatalogDatabase,
    ) {
      val actualCatalog = createActualDb(dbFile)
      val databaseComparator = ObjectDifferDatabaseComparator(
        ignoreDefinitions = !parameters.verifyDefinitions.get(),
        circularReferenceExceptionLogger = {
          logger.debug(it)
        },
      )
      val diffReport = databaseComparator.compare(currentDb, actualCatalog).let { diff ->
        buildString(diff::printTo)
      }

      check(diffReport.isEmpty()) {
        "Error migrating from ${dbFile.name}, fresh database looks" +
          " different from migration database:\n$diffReport"
      }
    }

    private fun createActualDb(dbFile: File): CatalogDatabase {
      val version = dbFile.nameWithoutExtension.toInt()
      val copy = dbFile.copyTo(File(parameters.workingDirectory.get().asFile, dbFile.name))
      val initStatements = ArrayList<CatalogDatabase.InitStatement>()
      environment.forMigrationFiles { file ->
        check(file.name.any { it in '0'..'9' }) {
          "Migration files must have an integer value somewhere in their filename but ${file.name} does not."
        }
        if (version > file.version) return@forMigrationFiles
        file.sqlStmtList!!.stmtList.forEach {
          initStatements.add(
            CatalogDatabase.InitStatement(
              it.rawSqlText(),
              "Error compiling ${file.name}",
            ),
          )
        }
      }
      return CatalogDatabase.fromFile(copy.absolutePath, initStatements).also { copy.delete() }
    }

    private fun checkForGaps() {
      var lastMigrationVersion: Long? = null
      environment.forMigrationFiles {
        val actual = it.version
        val expected = lastMigrationVersion?.plus(1) ?: actual
        check(actual == expected) {
          "Gap in migrations detected. Expected migration $expected, got $actual."
        }

        lastMigrationVersion = actual
      }
    }

    private fun MapProperty<String, String>.toProperties(): Properties {
      val connectionProperties = Properties()
      get().forEach { (key, value) ->
        connectionProperties[key] = value
      }
      return connectionProperties
    }
  }
}

/**
 * Allows consumers to configure and register (with [DriverManager]) their custom drivers prior to
 * running migration verification task.
 */
interface DriverInitializer {
  fun execute(properties: SqlDelightDatabaseProperties, driverProperties: Properties)
}
