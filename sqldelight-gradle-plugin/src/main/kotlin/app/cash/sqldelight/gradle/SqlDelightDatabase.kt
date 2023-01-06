package app.cash.sqldelight.gradle

import app.cash.sqldelight.*
import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.lang.MigrationFileType
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.gradle.kotlin.Source
import app.cash.sqldelight.gradle.kotlin.sources
import app.cash.sqldelight.gradle.squash.MigrationSquashTask
import org.gradle.api.*
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File
import javax.inject.Inject

abstract class SqlDelightDatabase @Inject constructor(
  val project: Project,
  val name: String,
) {

  init {
    sourceFolders.convention(listOf("sqldelight"))
    deriveSchemaFromMigrations.convention(false)
    verifyMigrations.convention(false)
    migrationOutputFileFormat.convention(".sql")
    generateAsync.convention(false)
    treatNullAsUnknownForEquality.convention(false)
  }

  abstract val packageName: Property<String>
  abstract val schemaOutputDirectory: DirectoryProperty
  abstract val sourceFolders: ListProperty<String>
  abstract val deriveSchemaFromMigrations: Property<Boolean>
  abstract val verifyMigrations: Property<Boolean>
  abstract val migrationOutputDirectory: DirectoryProperty
  abstract val migrationOutputFileFormat: Property<String>
  abstract val generateAsync: Property<Boolean>

  internal abstract val dialect: Property<Dependency>
  internal val configuration = project.configurations.register("${name}DialectClasspath") {
    it.isCanBeConsumed = false
    it.isVisible = false
    it.dependencies.addLater(dialect.orElse(project.provider { 
      error("""
        A dialect is needed for SQLDelight. For example for sqlite:
      sqldelight {
        $name {
          dialect("app.cash.sqldelight:sqlite-3-18-dialect:$VERSION")
        }
      }
      """.trimIndent())
    }))
  }

  internal val moduleConfiguration = project.configurations.register("${name}ModuleClasspath") {
    it.isCanBeConsumed = false
    it.isVisible = false
  }

  fun module(module: Any) {
    project.dependencies.add(moduleConfiguration.name, module)
  }

  fun dialect(dialect: Any) {
    this.dialect.set(project.dependencies.create(dialect))
    this.dialect.finalizeValue()
  }

  fun dialect(dialect: Provider<Dependency>) {
    this.dialect.set(dialect)
    this.dialect.finalizeValue()
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

  private val generatedSourcesDirectory = project.layout.buildDirectory.dir("generated/sqldelight/code/$name")

  private val sources = sources(project)
  internal abstract val dependencies: NamedDomainObjectContainer<SqlDelightDatabase>

  @Suppress("unused") // Public API used in gradle files.
  fun dependency(delegatedProject: ProjectDependency) = dependency(delegatedProject.dependencyProject)

  @Suppress("unused") // Public API used in gradle files.
  fun dependency(dependencyProject: Project) {
    project.evaluationDependsOn(dependencyProject.path)

    val dependency = dependencyProject.extensions.findByType(SqlDelightExtension::class.java)
      ?: throw IllegalStateException("Cannot depend on a module with no sqldelight plugin.")
    val database = dependency.databases.named(name)
    
    dependencies.addLater(database.flatMap { dbToAdd ->
      val validatedPackageName = dbToAdd.packageName.zip(packageName) { otherDBPackageName, thisPackageName ->
        if (otherDBPackageName == thisPackageName) {
          error("Detected a schema that already has the package name $packageName in project $dependencyProject")
        } else dbToAdd
      }
      validatedPackageName.flatMap {
        "Found a circular dependency in $project with database $name"
        it.dependencies.elements(project.providers).
      }
    })
  }

  internal fun properties(): Provider<SqlDelightDatabasePropertiesImpl> = packageName.orElse(
    project.provider {
      error("property packageName for $name database must be provided")
    }
  ).map { packageName ->
    SqlDelightDatabasePropertiesImpl(
      packageName = packageName,
      compilationUnitsProvider = sources.map { sources ->
        sources.map { source ->
          SqlDelightCompilationUnitImpl(
            name = source.name,
            sourceDirectories = sourceFolders(source),
            outputDirectory = source.outputDir,
          )
        }
      },
      rootDirectory = project.projectDir,
      className = name,
      dependencies = dependencies.map { SqlDelightDatabaseNameImpl(it.packageName.get(), it.name) },
      deriveSchemaFromMigrations = deriveSchemaFromMigrations.get(),
      treatNullAsUnknownForEquality = treatNullAsUnknownForEquality.get(),
      generateAsync = generateAsync.get(),
    )
  }

  private fun sourceFolders(source: Source): Provider<List<SqlDelightSourceFolderImpl>> {
    val relativeSourceFolders = sourceFolders.map { sourceFolders ->
      sourceFolders.flatMap { folder ->
        source.sourceSets.map {
          SqlDelightSourceFolderImpl(
            directory = project.layout.projectDirectory.dir("src/$it/$folder"),
            dependency = false,
          )
        }
      }
    }
    
    val dependencyProvider = project.objects.listProperty(SqlDelightSourceFolderImpl::class.java) 
    for (dependency in dependencies) {
        val dependencySource = source.closestMatch(dependency.sources)

        dependencyProvider.addAll(dependencySource.zip(dependency.properties()) { dependencySource, properties ->
          val compilationUnit = properties.compilationUnits.single { it.name == dependencySource.name }
          compilationUnit.sourceFolders.map {
            SqlDelightSourceFolderImpl(
              directory = it.directory,
              dependency = true,
            )
          }
        })
      }

    return relativeSourceFolders.zip(dependencyProvider) { relativeSourceFolders, dependencies ->
      (relativeSourceFolders + dependencies).distinct()
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
        it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
        it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
        it.group = SqlDelightPlugin.GROUP
        it.description = "Generate ${source.name} Kotlin interface for $name"
        it.verifyMigrations.set(verifyMigrations)
        it.classpath.setFrom(configuration)
        it.classpath.from(moduleConfiguration)
      }

      val outputDirectoryProvider = task.flatMap { it.outputDirectory }

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

      
      addMigrationOutputTasks(migrationOutputDirectory, sourceFiles.files, source)
    }
  }

  private fun addMigrationTasks(
    sourceSet: Collection<File>,
    source: Source,
  ) {
    val verifyMigrationTask =
      project.tasks.register("verify${source.name.capitalize()}${name}Migration", VerifyMigrationTask::class.java) {
        it.projectName.set(project.name)
        it.compilationUnit.set(properties().map { it.compilationUnits.single { it.name == source.name } })
        it.source(sourceSet)
        it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
        it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
        it.workingDirectory.set(project.layout.buildDirectory.dir("sqldelight/migration_verification/${source.name.capitalize()}$name"))
        it.group = SqlDelightPlugin.GROUP
        it.description = "Verify ${source.name} $name migrations and CREATE statements match."
        it.properties.set(properties())
        it.verifyMigrations.set(verifyMigrations)
        it.classpath.setFrom(configuration)
        it.classpath.from(moduleConfiguration)
      }

    project.tasks.addLater(
      schemaOutputDirectory.flatMap { schemaOutputDirectory ->
        project.tasks.register("generate${source.name.capitalize()}${name}Schema", GenerateSchemaTask::class.java) {
          it.projectName.set(project.name)
          it.compilationUnit.set(properties().map { it.compilationUnits.single { it.name == source.name } })
          it.outputDirectory.set(schemaOutputDirectory)
          it.source(sourceSet)
          it.include("**${File.separatorChar}*.${SqlDelightFileType.defaultExtension}")
          it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
          it.group = SqlDelightPlugin.GROUP
          it.description = "Generate a .db file containing the current $name schema for ${source.name}."
          it.properties.set(properties())
          it.verifyMigrations.set(verifyMigrations)
          it.classpath.setFrom(configuration)
          it.classpath.from(moduleConfiguration)
        }
      })
    project.tasks.named("check").configure {
      it.dependsOn(verifyMigrationTask)
    }
    project.tasks.named("verifySqlDelightMigration").configure {
      it.dependsOn(verifyMigrationTask)
    }
  }

  private fun addMigrationOutputTasks(
    migrationOutputDirectory: Provider<Directory>,
    sourceSet: Collection<File>,
    source: Source,
  ) {
    project.tasks.addLater(
      migrationOutputDirectory.map { migrationOutputDirectory ->
        project.objects.named(
          GenerateMigrationOutputTask::class.java,
          "generate${source.name.capitalize()}${name}Migrations"
        ).apply {
          projectName.set(project.name)
          compilationUnit.set(properties().map { it.compilationUnits.single { it.name == source.name } })
          source(sourceSet)
          include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
          migrationOutputExtension.set(migrationOutputFileFormat)
          outputDirectory.set(migrationOutputDirectory)
          group = SqlDelightPlugin.GROUP
          description = "Generate valid sql migration files for ${source.name} $name."
          properties.set(properties())
          classpath.setFrom(configuration)
          classpath.from(moduleConfiguration)
        }
      }
    )
  }

  private fun addSquashTask(
    sourceSet: Collection<File>,
    source: Source,
  ) {
    project.tasks.register("squash${source.name.capitalize()}${name}Migrations", MigrationSquashTask::class.java) {
      it.projectName.set(project.name)
      it.compilationUnit.set(properties().map { it.compilationUnits.single { it.name == source.name } })
      it.source(sourceSet)
      it.include("**${File.separatorChar}*.${MigrationFileType.defaultExtension}")
      it.group = SqlDelightPlugin.GROUP
      it.description = "Squash migrations into a single file for ${source.name} $name."
      it.properties.set(properties())
      it.classpath.setFrom(configuration)
      it.classpath.from(moduleConfiguration)
    }
  }

  private val Source.outputDir get() = sources.flatMap {
    if (it.size > 1) generatedSourcesDirectory.map { it.dir(name) }
    else generatedSourcesDirectory
  }
}
