package app.cash.sqldelight.gradle

import app.cash.sqldelight.VERSION
import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.lang.MigrationFileType
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.gradle.kotlin.Source
import app.cash.sqldelight.gradle.kotlin.sources
import app.cash.sqldelight.gradle.squash.MigrationSquashTask
import groovy.lang.GroovyObject
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import java.io.File
import javax.inject.Inject

abstract class SqlDelightDatabase @Inject constructor(
  val project: Project,
  val name: String,
) {

  init {
    deriveSchemaFromMigrations.convention(false)
    verifyMigrations.convention(false)
    migrationOutputFileFormat.convention(".sql")
    generateAsync.convention(false)
    treatNullAsUnknownForEquality.convention(false)
  }

  abstract val packageName: Property<String>
  abstract val schemaOutputDirectory: DirectoryProperty
  abstract val srcDirs: ConfigurableFileCollection
  abstract val deriveSchemaFromMigrations: Property<Boolean>
  abstract val verifyMigrations: Property<Boolean>
  abstract val migrationOutputDirectory: DirectoryProperty
  abstract val migrationOutputFileFormat: Property<String>
  abstract val generateAsync: Property<Boolean>

  internal val configuration = project.configurations.create("${name}DialectClasspath").apply {
    isCanBeConsumed = false
    isVisible = false
  }

  internal val moduleConfiguration = project.configurations.create("${name}ModuleClasspath").apply {
    isCanBeConsumed = false
    isVisible = false
  }

  internal var addedDialect: Boolean = false

  fun module(module: Any) {
    moduleConfiguration.dependencies.add(project.dependencies.create(module))
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
  abstract val treatNullAsUnknownForEquality: Property<Boolean>

  private val generatedSourcesDirectory
    get() = File(project.buildDir, "generated/sqldelight/code/$name")

  private val sources by lazy { sources(project) }
  // Mapping of dependencies to their parent project
  private val dependencies = mutableMapOf<SqlDelightDatabase, Project>()

  private var recursionGuard = false

  fun methodMissing(name: String, args: Any): Any {
    return (project as GroovyObject).invokeMethod(name, args)
  }

  @Suppress("unused") // Public API used in gradle files.
  fun dependency(delegatedProject: ProjectDependency) = dependency(delegatedProject.dependencyProject)

  @Suppress("unused") // Public API used in gradle files.
  fun dependency(dependencyProject: Project) {
    project.evaluationDependsOn(dependencyProject.path)

    val dependency = dependencyProject.extensions.findByType(SqlDelightExtension::class.java)
      ?: throw IllegalStateException("Cannot depend on a module with no sqldelight plugin.")
    val database = dependency.databases.singleOrNull { it.name == name }
      ?: throw IllegalStateException("No database named $name in $dependencyProject")
    dependencies[database] = dependencyProject
  }

  fun srcDirs(vararg srcPaths: Any) {
    srcDirs.from(srcPaths)
  }

  internal fun getProperties(): SqlDelightDatabasePropertiesImpl {
    val packageName = requireNotNull(packageName.getOrNull()) { "property packageName for $name database must be provided" }

    check(!recursionGuard) { "Found a circular dependency in $project with database $name" }
    recursionGuard = true

    if (!addedDialect) throw GradleException(
      """
      A dialect is needed for SQLDelight. For example for sqlite:

      sqldelight {
        $name {
          dialect("app.cash.sqldelight:sqlite-3-18-dialect:$VERSION")
        }
      }
      """.trimIndent(),
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
        dependencies = dependencies.map { (db, project) ->
          val otherPackageName = db.packageName.get()
          check(otherPackageName != packageName) { "Detected a schema that already has the package name $packageName in project ${project.path}" }
          SqlDelightDatabaseNameImpl(otherPackageName, db.name)
        },
        deriveSchemaFromMigrations = deriveSchemaFromMigrations.get(),
        treatNullAsUnknownForEquality = treatNullAsUnknownForEquality.get(),
        generateAsync = generateAsync.get(),
      )
    } finally {
      recursionGuard = false
    }
  }

  private fun sourceFolders(source: Source): List<SqlDelightSourceFolderImpl> {
    val sourceFolders: List<SqlDelightSourceFolderImpl> = buildList {
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

    return sourceFolders + dependencies.flatMap { (dependency, _) ->
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
    }.distinct()
  }

  /**
   * Helper to wire relevant task dependencies for dependent projects' tasks. This is necessary to work in
   * Gradle 8.0+, where otherwise-implicit task dependencies are not allowed.
   */
  private fun <T : Task> wireDependencyTaskDependencies(
    task: T,
    clazz: Class<T>,
    nameMatches: (db: SqlDelightDatabase, otherTaskName: String) -> Boolean
  ) {
    // Note that because the dependency DB doesn't indicate a specific source/variant, we just depend on all of
    // them to be safe.
    for ((db, dbProject) in dependencies) {
      // TaskContainer.withType() and matching() both return live collections
      task.dependsOn(dbProject.tasks.withType(clazz).matching { nameMatches(db, it.name) })
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
        it.outputDirectory = source.outputDir
        it.source(sourceFiles)
        it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
        it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
        it.group = SqlDelightPlugin.GROUP
        it.description = "Generate ${source.name} Kotlin interface for $name"
        it.verifyMigrations = verifyMigrations.get()
        it.classpath.setFrom(configuration.fileCollection { true })
        it.classpath.from(moduleConfiguration.fileCollection { true })

        wireDependencyTaskDependencies(it, SqlDelightTask::class.java) { db, otherProjectSchemaTaskName ->
          otherProjectSchemaTaskName
            .endsWith("${db.name}Interface")
        }
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

      if (!deriveSchemaFromMigrations.get()) {
        addMigrationTasks(sourceFiles.files, source)
      }

      if (deriveSchemaFromMigrations.get()) {
        addSquashTask(sourceFiles.files, source)
      }

      if (migrationOutputDirectory.getOrNull() != null) {
        addMigrationOutputTasks(sourceFiles.files, source)
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
        it.verifyMigrations = verifyMigrations.get()
        it.classpath.setFrom(configuration.fileCollection { true })
        it.classpath.from(moduleConfiguration.fileCollection { true })
      }

    if (schemaOutputDirectory.getOrNull() != null) {
      val properties = getProperties()
      project.tasks.register("generate${source.name.capitalize()}${name}Schema", GenerateSchemaTask::class.java) {
        it.projectName.set(project.name)
        it.compilationUnit = properties.compilationUnits.single { it.name == source.name }
        it.outputDirectory = schemaOutputDirectory.get().asFile
        it.source(sourceSet)
        it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
        it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
        it.group = SqlDelightPlugin.GROUP
        it.description = "Generate a .db file containing the current $name schema for ${source.name}."
        it.properties = properties
        it.verifyMigrations = verifyMigrations.get()
        it.classpath.setFrom(configuration.fileCollection { true })
        it.classpath.from(moduleConfiguration.fileCollection { true })

        wireDependencyTaskDependencies(it, GenerateSchemaTask::class.java) { db, otherProjectSchemaTaskName ->
          otherProjectSchemaTaskName
            .endsWith("${db.name}Schema")
        }
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
      it.migrationOutputExtension = migrationOutputFileFormat.get()
      it.outputDirectory = migrationOutputDirectory.get().asFile
      it.group = SqlDelightPlugin.GROUP
      it.description = "Generate valid sql migration files for ${source.name} $name."
      it.properties = getProperties()
      it.classpath.setFrom(configuration.fileCollection { true })
      it.classpath.from(moduleConfiguration.fileCollection { true })
    }
  }

  private fun addSquashTask(
    sourceSet: Collection<File>,
    source: Source,
  ) {
    project.tasks.register("squash${source.name.capitalize()}${name}Migrations", MigrationSquashTask::class.java) {
      it.projectName.set(project.name)
      it.compilationUnit = getProperties().compilationUnits.single { it.name == source.name }
      it.source(sourceSet)
      it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
      it.group = SqlDelightPlugin.GROUP
      it.description = "Squash migrations into a single file for ${source.name} $name."
      it.properties = getProperties()
      it.classpath.setFrom(configuration.fileCollection { true })
      it.classpath.from(moduleConfiguration.fileCollection { true })
    }
  }

  private val Source.outputDir get() =
    if (sources.size > 1) File(generatedSourcesDirectory, name)
    else generatedSourcesDirectory
}
