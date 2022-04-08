package app.cash.sqldelight.gradle

import app.cash.sqldelight.core.lang.MigrationFileType
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.gradle.kotlin.Source
import app.cash.sqldelight.gradle.kotlin.sources
import com.android.builder.model.AndroidProject.FD_GENERATED
import groovy.lang.GroovyObject
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.gradle.api.provider.Provider
import java.io.File

class SqlDelightDatabase(
  val project: Project,
  var name: String,
  var packageName: String? = null,
  var schemaOutputDirectory: File? = null,
  var sourceFolders: Collection<String>? = null,
  dialect: String? = null,
  var deriveSchemaFromMigrations: Boolean = false,
  var verifyMigrations: Boolean = false,
  var migrationOutputDirectory: File? = null,
  var migrationOutputFileFormat: String = ".sql",
) {
  internal val configuration = project.configurations.create("${name}DialectClasspath").apply {
    isTransitive = false
    if (dialect != null) dependencies.add(project.dependencies.create(dialect))
  }

  internal val moduleConfiguration = project.configurations.create("${name}ModuleClasspath").apply {
    isTransitive = false
  }

  var dialect: String? = dialect
    set(value) {
      if (field != null) throw IllegalStateException("Can only set a single dialect.")
      field = value
      if (value != null) configuration.dependencies.add(project.dependencies.create(value))
    }

  fun module(module: Any) {
    moduleConfiguration.dependencies.add(project.dependencies.create(module))
  }

  /**
   * When SqlDelight finds an equality operation with a nullable typed rvalue such as:
   *
   * ```
   * SELECT *
   * FROM test_table
   * WHERE foo = ?
   * ```
   *
   * It will generate:
   *
   * ```
   * private val foo: String?
   *
   * |SELECT *
   * |FROM test_table
   * |WHERE foo ${ if (foo == null) "IS" else "=" } ?1
   * ```
   *
   * The = operator is expected to return `false` if comparing to a value that is `null`.
   * However, the above code will return true when `foo` is `null`.
   *
   * By enabling [treatNullAsUnknownForEquality], the `null`
   * check will not be generated, resulting in correct SQL behavior:
   *
   * ```
   * private val foo: String?
   *
   * |SELECT *
   * |FROM test_table
   * |WHERE foo = ?1
   * ```
   *
   * @see <a href="https://github.com/cashapp/sqldelight/issues/1490">sqldelight#1490</a>
   * @see <a href="https://en.wikipedia.org/wiki/Null_%28SQL%29#Null-specific_and_3VL-specific_comparison_predicates">Wikipedia entry on null specific comparisons in SQL</a>
   */
  var treatNullAsUnknownForEquality: Boolean = false

  private val generatedSourcesDirectory
    get() = File(project.buildDir, "$FD_GENERATED/sqldelight/code/$name")

  private val sources by lazy { sources() }
  private val dependencies = mutableListOf<SqlDelightDatabase>()

  private var recursionGuard = false

  fun methodMissing(name: String, args: Any): Any {
    return (project as GroovyObject).invokeMethod(name, args)
  }

  @Suppress("unused") // Public API used in gradle files.
  fun dependency(delegatedProject: DelegatingProjectDependency) = dependency(delegatedProject.dependencyProject)

  @Suppress("unused") // Public API used in gradle files.
  fun dependency(dependencyProject: Project) {
    project.evaluationDependsOn(dependencyProject.path)

    val dependency = dependencyProject.extensions.findByType(SqlDelightExtension::class.java)
      ?: throw IllegalStateException("Cannot depend on a module with no sqldelight plugin.")
    val database = dependency.databases.singleOrNull { it.name == name }
      ?: throw IllegalStateException("No database named $name in $dependencyProject")
    check(database.packageName != packageName) { "Detected a schema that already has the package name $packageName in project $dependencyProject" }
    dependencies.add(database)
  }

  internal fun getProperties(): SqlDelightDatabasePropertiesImpl {
    val packageName = requireNotNull(packageName) { "property packageName for $name database must be provided" }

    check(!recursionGuard) { "Found a circular dependency in $project with database $name" }
    recursionGuard = true

    if (dialect == null) throw GradleException(
      """
      A dialect is needed for SQLDelight. For example for sqlite:

      sqldelight {
        $name {
          dialect = "sqlite:3.18"
        }
      }
      """.trimIndent()
    )

    try {
      return SqlDelightDatabasePropertiesImpl(
        packageName = packageName,
        compilationUnits = sources.map { source ->
          SqlDelightCompilationUnitImpl(
            name = source.name,
            sourceFolders = sourceFolders(source).sortedBy { it.folder.absolutePath },
            outputDirectoryFile = source.outputDir,
          )
        },
        rootDirectory = project.projectDir,
        className = name,
        dependencies = dependencies.map { SqlDelightDatabaseNameImpl(it.packageName!!, it.name) },
        deriveSchemaFromMigrations = deriveSchemaFromMigrations,
        treatNullAsUnknownForEquality = treatNullAsUnknownForEquality
      )
    } finally {
      recursionGuard = false
    }
  }

  private fun sourceFolders(source: Source): List<SqlDelightSourceFolderImpl> {
    val sourceFolders = sourceFolders ?: listOf("sqldelight")

    val relativeSourceFolders = sourceFolders.flatMap { folder ->
      source.sourceSets.map {
        SqlDelightSourceFolderImpl(
          folder = File(project.projectDir, "src/$it/$folder"),
          dependency = false
        )
      }
    }

    return relativeSourceFolders + dependencies.flatMap { dependency ->
      val dependencySource = source.closestMatch(dependency.sources)
        ?: return@flatMap emptyList<SqlDelightSourceFolderImpl>()
      val compilationUnit = dependency.getProperties().compilationUnits
        .single { it.name == dependencySource.name }

      return@flatMap compilationUnit.sourceFolders.map {
        SqlDelightSourceFolderImpl(
          folder = File(project.projectDir, project.relativePath(it.folder.absolutePath)),
          dependency = true
        )
      }
    }.distinct()
  }

  internal fun registerTasks() {
    sources.forEach { source ->
      val allFiles = sourceFolders(source)
      val sourceFiles = project.files(*allFiles.filter { !it.dependency }.map { it.folder }.toTypedArray())
      val dependencyFiles = project.files(*allFiles.filter { it.dependency }.map { it.folder }.toTypedArray())

      // Register the sqldelight generating task.
      val task = project.tasks.register("generate${source.name.capitalize()}${name}Interface", SqlDelightTask::class.java) {
        it.projectName.set(project.name)
        it.properties = getProperties()
        it.compilationUnit = getProperties().compilationUnits.single { it.name == source.name }
        it.outputDirectory = source.outputDir
        it.source(sourceFiles + dependencyFiles)
        it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
        it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
        it.group = SqlDelightPlugin.GROUP
        it.description = "Generate ${source.name} Kotlin interface for $name"
        it.verifyMigrations = verifyMigrations
        it.classpath.setFrom(configuration.fileCollection { true })
        it.classpath.from(moduleConfiguration.fileCollection { true })
      }

      val outputDirectoryProvider: Provider<File> = task.map { it.outputDirectory!! }

      // Add the source dependency on the generated code.
      // Use a Provider generated from the task to carry task dependencies
      // See https://github.com/cashapp/sqldelight/issues/2119
      source.sourceDirectorySet.srcDir(outputDirectoryProvider)
      // And register the output directory to the IDE if needed
      source.registerGeneratedDirectory?.invoke(outputDirectoryProvider)

      project.tasks.named("generateSqlDelightInterface").configure {
        it.dependsOn(task)
      }

      if (!deriveSchemaFromMigrations) {
        addMigrationTasks(sourceFiles.files + dependencyFiles.files, source)
      }

      if (migrationOutputDirectory != null) {
        addMigrationOutputTasks(sourceFiles.files + dependencyFiles.files, source)
      }
    }
  }

  private fun addMigrationTasks(
    sourceSet: Collection<File>,
    source: Source,
  ) {
    val verifyMigrationTask =
      project.tasks.register("verify${source.name.capitalize()}${name}Migration", VerifyMigrationTask::class.java) {
        it.projectName.set(project.name)
        it.compilationUnit = getProperties().compilationUnits.single { it.name == source.name }
        it.source(sourceSet)
        it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
        it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
        it.workingDirectory = File(project.buildDir, "sqldelight/migration_verification/${source.name.capitalize()}$name")
        it.group = SqlDelightPlugin.GROUP
        it.description = "Verify ${source.name} $name migrations and CREATE statements match."
        it.properties = getProperties()
        it.verifyMigrations = verifyMigrations
        it.classpath.setFrom(configuration.fileCollection { true })
        it.classpath.from(moduleConfiguration.fileCollection { true })
      }

    if (schemaOutputDirectory != null) {
      project.tasks.register("generate${source.name.capitalize()}${name}Schema", GenerateSchemaTask::class.java) {
        it.projectName.set(project.name)
        it.compilationUnit = getProperties().compilationUnits.single { it.name == source.name }
        it.outputDirectory = schemaOutputDirectory
        it.source(sourceSet)
        it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
        it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
        it.group = SqlDelightPlugin.GROUP
        it.description = "Generate a .db file containing the current $name schema for ${source.name}."
        it.properties = getProperties()
        it.verifyMigrations = verifyMigrations
        it.classpath.setFrom(configuration.fileCollection { true })
        it.classpath.from(moduleConfiguration.fileCollection { true })
      }
    }
    project.tasks.named("check").configure {
      it.dependsOn(verifyMigrationTask)
    }
    project.tasks.named("verifySqlDelightMigration").configure {
      it.dependsOn(verifyMigrationTask)
    }
  }

  private fun addMigrationOutputTasks(
    sourceSet: Collection<File>,
    source: Source,
  ) {
    project.tasks.register("generate${source.name.capitalize()}${name}Migrations", GenerateMigrationOutputTask::class.java) {
      it.projectName.set(project.name)
      it.compilationUnit = getProperties().compilationUnits.single { it.name == source.name }
      it.source(sourceSet)
      it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
      it.migrationOutputExtension = migrationOutputFileFormat
      it.outputDirectory = migrationOutputDirectory
      it.group = SqlDelightPlugin.GROUP
      it.description = "Generate valid sql migration files for ${source.name} $name."
      it.properties = getProperties()
      it.classpath.setFrom(configuration.fileCollection { true })
      it.classpath.from(moduleConfiguration.fileCollection { true })
    }
  }

  private val Source.outputDir get() =
    if (sources.size > 1) File(generatedSourcesDirectory, name)
    else generatedSourcesDirectory
}
