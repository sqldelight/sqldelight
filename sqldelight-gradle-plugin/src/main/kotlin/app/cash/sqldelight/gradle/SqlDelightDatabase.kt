package app.cash.sqldelight.gradle

import app.cash.sqldelight.VERSION
import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.lang.MIGRATION_EXTENSION
import app.cash.sqldelight.core.lang.SQLDELIGHT_EXTENSION
import app.cash.sqldelight.gradle.kotlin.Source
import app.cash.sqldelight.gradle.kotlin.sources
import app.cash.sqldelight.gradle.squash.MigrationSquashTask
import groovy.lang.GroovyObject
import java.io.File
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

@SqlDelightDsl
abstract class SqlDelightDatabase @Inject constructor(
  val project: Project,
  val name: String,
) {

  abstract val packageName: Property<String>
  abstract val schemaOutputDirectory: DirectoryProperty
  abstract val srcDirs: ConfigurableFileCollection
  val deriveSchemaFromMigrations: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)
  val verifyMigrations: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)
  val verifyDefinitions: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)
  abstract val migrationOutputDirectory: DirectoryProperty
  val migrationOutputFileFormat: Property<String> = project.objects.property(String::class.java).convention(".sql")
  val generateAsync: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

  val configurationName: String = "${name}DialectClasspath"

  internal val configuration = project.configurations.create(configurationName).apply {
    isCanBeConsumed = false
    isVisible = false
  }

  private val intellijEnv = project.configurations.create("${name}IntellijEnv").apply {
    isCanBeConsumed = false
    isVisible = false
    dependencies.add(project.dependencies.create("app.cash.sqldelight:compiler-env:$VERSION"))
  }

  private val migrationEnv = project.configurations.create("${name}MigrationEnv").apply {
    isCanBeConsumed = false
    isVisible = false
    dependencies.add(project.dependencies.create("app.cash.sqldelight:migration-env:$VERSION"))
  }

  internal var addedDialect: Boolean = false

  fun module(module: Any) {
    configuration.dependencies.add(project.dependencies.create(module))
  }

  fun dialect(dialect: Any) {
    if (addedDialect) throw IllegalStateException("Can only set a single dialect.")
    project.dependencies.add(configuration.name, dialect)
    addedDialect = true
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
  val treatNullAsUnknownForEquality: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

  private val generatedSourcesDirectory
    get() = File(project.buildDir, "generated/sqldelight/code/$name")

  private val sources by lazy { sources(project) }
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
    check(database.packageName.get() != packageName.get()) { "Detected a schema that already has the package name ${packageName.get()} in project $dependencyProject" }
    dependencies.add(database)
  }

  fun srcDirs(vararg srcPaths: Any) {
    srcDirs.from(srcPaths)
  }

  internal fun getProperties(): SqlDelightDatabasePropertiesImpl {
    require(packageName.isPresent) { "property packageName for $name database must be provided" }

    check(!recursionGuard) { "Found a circular dependency in $project with database $name" }
    recursionGuard = true

    if (!addedDialect) {
      throw GradleException(
        """
      A dialect is needed for SQLDelight. For example for sqlite:

      sqldelight {
        $name {
          dialect("app.cash.sqldelight:sqlite-3-18-dialect:$VERSION")
        }
      }
        """.trimIndent(),
      )
    }

    try {
      return SqlDelightDatabasePropertiesImpl(
        packageName = packageName.get(),
        compilationUnits = sources.map { source ->
          SqlDelightCompilationUnitImpl(
            name = source.name,
            sourceFolders = sourceFolders(source),
            outputDirectoryFile = source.outputDir,
          )
        },
        rootDirectory = project.projectDir,
        className = name,
        dependencies = dependencies.map { SqlDelightDatabaseNameImpl(it.packageName.get(), it.name) },
        deriveSchemaFromMigrations = deriveSchemaFromMigrations.get(),
        treatNullAsUnknownForEquality = treatNullAsUnknownForEquality.get(),
        generateAsync = generateAsync.get(),
      )
    } finally {
      recursionGuard = false
    }
  }

  private fun sourceFolders(source: Source): Set<SqlDelightSourceFolderImpl> {
    val sourceFolders: Set<SqlDelightSourceFolderImpl> = buildSet {
      for (dir in srcDirs) {
        val sqlDelightSourceFolder = SqlDelightSourceFolderImpl(folder = dir, dependency = false)
        add(sqlDelightSourceFolder)
      }

      if (this.isEmpty()) {
        for (sourceSetName in source.sourceSets) {
          val sqlDelightSourceFolder = SqlDelightSourceFolderImpl(
            folder = File(project.projectDir, "src/$sourceSetName/sqldelight"),
            dependency = false,
          )
          add(sqlDelightSourceFolder)
        }
      }
    }

    return sourceFolders + dependencies.flatMap { dependency ->
      val dependencySource = source.closestMatch(dependency.sources)
        ?: return@flatMap emptyList<SqlDelightSourceFolderImpl>()
      val compilationUnit = dependency.getProperties().compilationUnits
        .single { it.name == dependencySource.name }

      return@flatMap compilationUnit.sourceFolders.map {
        SqlDelightSourceFolderImpl(
          folder = File(project.projectDir, project.relativePath(it.folder.absolutePath)),
          dependency = true,
        )
      }
    }
  }

  internal fun registerTasks() {
    sources.forEach { source ->
      val allFiles = sourceFolders(source)
      val sourceFiles = project.files(*allFiles.map { it.folder }.toTypedArray())

      // Register the sqldelight generating task.
      val task = project.tasks.register("generate${source.name.capitalize()}${name}Interface", SqlDelightTask::class.java) {
        it.projectName.set(project.name)
        it.properties = getProperties()
        it.compilationUnit = getProperties().compilationUnits.single { it.name == source.name }
        it.outputDirectory.set(source.outputDir)
        it.source(sourceFiles)
        it.include("**${File.separatorChar}*.$SQLDELIGHT_EXTENSION")
        it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
        it.group = SqlDelightPlugin.GROUP
        it.description = "Generate ${source.name} Kotlin interface for $name"
        it.verifyMigrations.set(verifyMigrations)
        it.classpath.setFrom(intellijEnv, migrationEnv, configuration)
      }

      val outputDirectoryProvider: Provider<File> = task.flatMap { it.outputDirectory.asFile }

      // Add the source dependency on the generated code.
      // Use a Provider generated from the task to carry task dependencies
      // See https://github.com/cashapp/sqldelight/issues/2119
      source.sourceDirectorySet.srcDir(task)
      // And register the output directory to the IDE if needed
      source.registerGeneratedDirectory?.invoke(outputDirectoryProvider)

      project.tasks.named("generateSqlDelightInterface").configure {
        it.dependsOn(task)
      }

      if (!deriveSchemaFromMigrations.get()) {
        addMigrationTasks(sourceFiles, source)
      }

      if (deriveSchemaFromMigrations.get()) {
        addSquashTask(sourceFiles, source)
      }

      if (migrationOutputDirectory.isPresent) {
        addMigrationOutputTasks(sourceFiles, source)
      }
    }
  }

  private fun addMigrationTasks(
    sourceSet: FileCollection,
    source: Source,
  ) {
    val verifyMigrationTask =
      project.tasks.register("verify${source.name.capitalize()}${name}Migration", VerifyMigrationTask::class.java) {
        it.projectName.set(project.name)
        it.compilationUnit = getProperties().compilationUnits.single { it.name == source.name }
        it.source(sourceSet)
        it.include("**${File.separatorChar}*.$SQLDELIGHT_EXTENSION")
        it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
        it.workingDirectory.set(File(project.buildDir, "sqldelight/migration_verification/${source.name.capitalize()}$name"))
        it.group = SqlDelightPlugin.GROUP
        it.description = "Verify ${source.name} $name migrations and CREATE statements match."
        it.properties = getProperties()
        it.verifyMigrations.set(verifyMigrations)
        it.verifyDefinitions.set(verifyDefinitions)
        it.classpath.setFrom(intellijEnv, migrationEnv, configuration)
      }

    if (schemaOutputDirectory.isPresent) {
      project.tasks.register("generate${source.name.capitalize()}${name}Schema", GenerateSchemaTask::class.java) {
        it.projectName.set(project.name)
        it.compilationUnit = getProperties().compilationUnits.single { it.name == source.name }
        it.outputDirectory.set(schemaOutputDirectory)
        it.source(sourceSet)
        it.include("**${File.separatorChar}*.$SQLDELIGHT_EXTENSION")
        it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
        it.group = SqlDelightPlugin.GROUP
        it.description = "Generate a .db file containing the current $name schema for ${source.name}."
        it.properties = getProperties()
        it.verifyMigrations.set(verifyMigrations)
        it.classpath.setFrom(intellijEnv, migrationEnv, configuration)
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
    sourceSet: FileCollection,
    source: Source,
  ) {
    project.tasks.register("generate${source.name.capitalize()}${name}Migrations", GenerateMigrationOutputTask::class.java) {
      it.projectName.set(project.name)
      it.compilationUnit = getProperties().compilationUnits.single { it.name == source.name }
      it.source(sourceSet)
      it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
      it.migrationOutputExtension.set(migrationOutputFileFormat)
      it.outputDirectory.set(migrationOutputDirectory)
      it.group = SqlDelightPlugin.GROUP
      it.description = "Generate valid sql migration files for ${source.name} $name."
      it.properties = getProperties()
      it.classpath.setFrom(intellijEnv, configuration)
    }
  }

  private fun addSquashTask(
    sourceSet: FileCollection,
    source: Source,
  ) {
    project.tasks.register("squash${source.name.capitalize()}${name}Migrations", MigrationSquashTask::class.java) {
      it.projectName.set(project.name)
      it.compilationUnit = getProperties().compilationUnits.single { it.name == source.name }
      it.source(sourceSet)
      it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
      it.group = SqlDelightPlugin.GROUP
      it.description = "Squash migrations into a single file for ${source.name} $name."
      it.properties = getProperties()
      it.classpath.setFrom(intellijEnv, configuration)
    }
  }

  private val Source.outputDir get() = File(generatedSourcesDirectory, name)
}
