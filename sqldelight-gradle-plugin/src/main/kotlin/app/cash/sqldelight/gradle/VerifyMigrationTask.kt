package app.cash.sqldelight.gradle

import app.cash.sqldelight.VERSION
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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import java.io.File
import java.util.ServiceLoader
import javax.inject.Inject

@Suppress("UnstableApiUsage") // Worker API
@CacheableTask
abstract class VerifyMigrationTask : SqlDelightWorkerTask() {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input val pluginVersion = VERSION

  @get:Inject
  abstract override val workerExecutor: WorkerExecutor

  @Input val projectName: Property<String> = project.objects.property(String::class.java)

  /** Directory where the database files are copied for the migration scripts to run against. */
  @Internal lateinit var workingDirectory: File

  @Nested lateinit var properties: SqlDelightDatabasePropertiesImpl
  @Nested lateinit var compilationUnit: SqlDelightCompilationUnitImpl

  @Input var verifyMigrations: Boolean = false

  @get:OutputDirectory
  internal val outputFolder: File = File(project.buildDir, "sqldelight")

  companion object {
    private const val schemaJson = "schema.json"
    private const val schema = "schema.schema"
  }

  @TaskAction
  fun verifyMigrations() {
    val workQueue = workQueue()
    workQueue.submit(VerifyMigrationAction::class.java) {
      it.workingDirectory.set(workingDirectory)
      it.projectName.set(projectName)
      it.properties.set(properties)
      it.verifyMigrations.set(verifyMigrations)
      it.compilationUnit.set(compilationUnit)
    }
    workQueue.await()

    outputFolder.ensureParentDirsCreated()
    schema.moveDBSchema()
    schemaJson.moveDBSchema()
    workingDirectory.deleteRecursively()
  }

  private fun String.moveDBSchema() {
    File(workingDirectory, this).let {
      val dbName = it.parentFile.name
      it.copyTo(File(outputFolder, "$dbName/$this"))
      it.delete()
    }
  }

  @InputFiles
  @SkipWhenEmpty
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
        dialect = ServiceLoader.load(SqlDelightDialect::class.java).findFirst().get(),
      )
    }

    override fun execute() {
      val workingDirectory = parameters.workingDirectory.get().asFile
      workingDirectory.deleteRecursively()
      workingDirectory.mkdirs()

      val catalog = createCurrentDb()

      val databaseFiles = sourceFolders.asSequence()
        .findDatabaseFiles()

      check(!parameters.verifyMigrations.get() || databaseFiles.count() > 0) {
        "Verifying a migration requires a database file to be present. To generate one, use the generate Gradle task."
      }

      databaseFiles.forEach { dbFile ->
        checkMigration(dbFile, catalog)
      }

      checkForGaps()
      saveSchema(catalog, workingDirectory)
    }

    private fun saveSchema(catalog: CatalogDatabase, workingDirectory: File) {
      val schemaFile = File(workingDirectory, schema)
      schemaFile.createNewFile()
      catalog.serialize(schemaFile)

      val schemaJsonFile = File(workingDirectory, schemaJson)
      schemaJsonFile.createNewFile()
      catalog.toJson(schemaJsonFile)
    }

    private fun createCurrentDb(): CatalogDatabase {
      val sourceFiles = ArrayList<SqlDelightQueriesFile>()
      environment.forSourceFiles { file ->
        if (file is SqlDelightQueriesFile) sourceFiles.add(file)
      }
      val initStatements = ArrayList<CatalogDatabase.InitStatement>()
      sourceFiles.forInitializationStatements(
        environment.dialect.allowsReferenceCycles
      ) { sqlText ->
        initStatements.add(CatalogDatabase.InitStatement(sqlText, "Error compiling $sqlText"))
      }
      return CatalogDatabase.withInitStatements(initStatements)
    }

    private fun checkMigration(dbFile: File, currentDb: CatalogDatabase) {
      val actualCatalog = createActualDb(dbFile)
      val databaseComparator = ObjectDifferDatabaseComparator(
        circularReferenceExceptionLogger = {
          logger.debug(it)
        }
      )
      val diffReport = buildString {
        val diff = databaseComparator.compare(currentDb, actualCatalog)
        diff.printTo(this)
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
              "Error compiling ${file.name}"
            )
          )
        }
      }
      return CatalogDatabase.fromFile(copy.absolutePath, initStatements).also { copy.delete() }
    }

    private fun checkForGaps() {
      var lastMigrationVersion: Int? = null
      environment.forMigrationFiles {
        val actual = it.version
        val expected = lastMigrationVersion?.plus(1) ?: actual
        check(actual == expected) {
          "Gap in migrations detected. Expected migration $expected, got $actual."
        }

        lastMigrationVersion = actual
      }
    }
  }
}
