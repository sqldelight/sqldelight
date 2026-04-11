package app.cash.sqldelight.gradle

import app.cash.sqldelight.VERSION
import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.lang.MIGRATION_EXTENSION
import app.cash.sqldelight.core.lang.SQLDELIGHT_EXTENSION
import app.cash.sqldelight.gradle.kotlin.Source
import app.cash.sqldelight.gradle.kotlin.configureOnSources
import app.cash.sqldelight.gradle.squash.MigrationSquashTask
import groovy.lang.GroovyObject
import java.io.File
import javax.inject.Inject
import kotlin.DeprecationLevel.HIDDEN
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

private const val SQLDELIGHT_SCHEMA_USAGE = "sqldelight-schema"

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

  val expandSelectStar: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)

  internal val configuration = project.configurations.create(configurationName).apply {
    isCanBeConsumed = false
    isVisible = false
    exclude(mapOf("group" to "org.jetbrains.kotlin", "module" to "kotlin-stdlib"))
    exclude(mapOf("group" to "org.jetbrains.kotlin", "module" to "kotlin-reflect"))
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

  private val dependencyScope = project.configurations.register("sqlDelightDatabase${name}") {
    it.isCanBeConsumed = false
    it.isCanBeResolved = false
  }

  private val dependenciesProjects =
    project.configurations.register("sqlDelightDatabase${name}Dependencies") {
      it.isCanBeConsumed = false
      it.isCanBeResolved = true
      it.isVisible = false
      it.extendsFrom(dependencyScope.get())
      it.attributes.attribute(
        Usage.USAGE_ATTRIBUTE,
        project.objects.named(Usage::class.java, SQLDELIGHT_SCHEMA_USAGE),
      )
      it.attributes.attribute(
        SqlDelightDatabaseNameAttribute.ATTR,
        project.objects.named(SqlDelightDatabaseNameAttribute::class.java, name),
      )
      it.attributes.attributeProvider(
        SqlDelightPackageAttribute.ATTR,
        packageName.map {
          project.objects.named(SqlDelightPackageAttribute::class.java, it)
        },
      )
    }

  private val consumedProjectDependency =
    project.configurations.register("sqlDelightDatabase${name}Elements") {
      it.extendsFrom(dependenciesProjects.get())
      it.isCanBeConsumed = true
      it.isCanBeResolved = false
      it.isVisible = false
      it.attributes.attribute(
        Usage.USAGE_ATTRIBUTE,
        project.objects.named(Usage::class.java, SQLDELIGHT_SCHEMA_USAGE),
      )
      it.attributes.attribute(
        SqlDelightDatabaseNameAttribute.ATTR,
        project.objects.named(SqlDelightDatabaseNameAttribute::class.java, name),
      )
      it.attributes.attributeProvider(
        SqlDelightPackageAttribute.ATTR,
        packageName.map {
          project.objects.named(SqlDelightPackageAttribute::class.java, it)
        },
      )
    }

  internal var addedDialect: Boolean = false

  fun module(module: Any) {
    project.dependencies.add(configuration.name, module)
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

  private val dependencies = mutableListOf<SqlDelightDatabase>()

  private var recursionGuard = false

  private val sourceCollector = SqlDelightSourceCollector()

  fun methodMissing(name: String, args: Any): Any {
    return (project as GroovyObject).invokeMethod(name, args)
  }

  @Suppress("unused") // Public API used in gradle files.
  @Deprecated("use ProjectDependency", level = HIDDEN)
  fun dependency(delegatedProject: DelegatingProjectDependency) = dependency(project.project(delegatedProject.path))

  @Suppress("unused") // Public API used in gradle files.
  fun dependency(delegatedProject: ProjectDependency) = dependency(project.project(delegatedProject.path))

  @Suppress("unused") // Public API used in gradle files.
  fun dependency(dependencyProject: Project) {
    dependencyScope.configure {
      it.dependencies.add(project.dependencies.create(dependencyProject))
    }
  }

  fun srcDirs(vararg srcPaths: Any) {
    srcDirs.from(srcPaths)
  }

  internal fun getProperties(): Provider<SqlDelightDatabasePropertiesImpl> {
    return sourceCollector.databaseProperties()
  }

  internal fun registerTasks() {
    configureOnSources { source ->
      registerSourceAsVariant(source)
      val allFiles = sourceCollector.sourceFolders(source)
      val sourceFiles = project.files(allFiles.map { folders -> folders.map { it.folder } })

      // Register the sqldelight generating task.
      val task = project.tasks.register("generate${source.name.capitalize()}${name}Interface", SqlDelightTask::class.java) {
        it.projectName.set(project.name)
        it.properties.set(sourceCollector.databaseProperties())
        it.compilationUnit.set(sourceCollector.compilationUnits().map { units -> units.single { unit -> unit.name == source.name } })
        it.outputDirectory.set(source.outputDir)
        it.source(sourceFiles)
        it.include("**${File.separatorChar}*.$SQLDELIGHT_EXTENSION")
        it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
        it.group = SqlDelightPlugin.GROUP
        it.description = "Generate ${source.name} Kotlin interface for $name"
        it.verifyMigrations.set(verifyMigrations)
        it.classpath.setFrom(intellijEnv, migrationEnv, configuration)
      }

      // Register the generated output of the task as needed
      source.registerSourceGeneration(task)

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
        it.compilationUnit.set(sourceCollector.compilationUnits().map { units -> units.single { unit -> unit.name == source.name } })
        it.source(sourceSet)
        it.include("**${File.separatorChar}*.$SQLDELIGHT_EXTENSION")
        it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
        it.workingDirectory.set(File(project.buildDir, "sqldelight/migration_verification/${source.name.capitalize()}$name"))
        it.group = SqlDelightPlugin.GROUP
        it.description = "Verify ${source.name} $name migrations and CREATE statements match."
        it.properties.set(sourceCollector.databaseProperties())
        it.verifyMigrations.set(verifyMigrations)
        it.verifyDefinitions.set(verifyDefinitions)
        it.classpath.setFrom(intellijEnv, migrationEnv, configuration)
      }

    if (schemaOutputDirectory.isPresent) {
      project.tasks.register("generate${source.name.capitalize()}${name}Schema", GenerateSchemaTask::class.java) {
        it.projectName.set(project.name)
        it.compilationUnit.set(sourceCollector.compilationUnits().map { units -> units.single { unit -> unit.name == source.name } })
        it.outputDirectory.set(schemaOutputDirectory)
        it.source(sourceSet)
        it.include("**${File.separatorChar}*.$SQLDELIGHT_EXTENSION")
        it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
        it.group = SqlDelightPlugin.GROUP
        it.description = "Generate a .db file containing the current $name schema for ${source.name}."
        it.properties.set(sourceCollector.databaseProperties())
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
      it.compilationUnit.set(sourceCollector.compilationUnits().map { units -> units.single { unit -> unit.name == source.name } })
      it.source(sourceSet)
      it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
      it.migrationOutputExtension.set(migrationOutputFileFormat)
      it.outputDirectory.set(migrationOutputDirectory)
      it.group = SqlDelightPlugin.GROUP
      it.description = "Generate valid sql migration files for ${source.name} $name."
      it.properties.set(sourceCollector.databaseProperties())
      it.classpath.setFrom(intellijEnv, configuration)
    }
  }

  private fun addSquashTask(
    sourceSet: FileCollection,
    source: Source,
  ) {
    project.tasks.register("squash${source.name.capitalize()}${name}Migrations", MigrationSquashTask::class.java) {
      it.projectName.set(project.name)
      it.compilationUnit.set(sourceCollector.compilationUnits().map { units -> units.single { unit -> unit.name == source.name } })
      it.source(sourceSet)
      it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
      it.group = SqlDelightPlugin.GROUP
      it.description = "Squash migrations into a single file for ${source.name} $name."
      it.properties.set(sourceCollector.databaseProperties())
      it.classpath.setFrom(intellijEnv, configuration)
    }
  }

  private fun registerSourceAsVariant(source: Source) {
    consumedProjectDependency.get().outgoing { configurationPublications ->
      configurationPublications.variants.create(
        source.name,
      ) { variant ->
        source.sourceDirectories.get().forEach {
          variant.artifact(it)
        }
        variant.attributes.attribute(KotlinPlatformType.attribute, source.type)
      }
    }
  }

  private val Source.outputDir get() = File(generatedSourcesDirectory, name)

  // Collects compilation units as sourceFolders are defined so that they can be lazily provided to downstream consumers
  private inner class SqlDelightSourceCollector {
    private val compilationUnits = mutableMapOf<Source.Key, Provider<SqlDelightCompilationUnitImpl>>()

    fun databaseProperties(): Provider<SqlDelightDatabasePropertiesImpl> {
      require(packageName.isPresent) { "property packageName for $name database must be provided" }

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
        return project.provider {
          SqlDelightDatabasePropertiesImpl(
            packageName = packageName.get(),
            compilationUnits = compilationUnits().get(),
            rootDirectory = project.projectDir,
            className = name,
            dependencies = dependenciesProjects.get().incoming.artifacts.artifacts.map {
              SqlDelightDatabaseNameImpl(
                it.variant.latest().attributes.getAttribute(SqlDelightPackageAttribute.ATTR)!!.name,
                it.variant.latest().attributes.getAttribute(SqlDelightDatabaseNameAttribute.ATTR)!!.name,
              )
            },
            deriveSchemaFromMigrations = deriveSchemaFromMigrations.get(),
            treatNullAsUnknownForEquality = treatNullAsUnknownForEquality.get(),
            generateAsync = generateAsync.get(),
            expandSelectStar = expandSelectStar.get(),
          )
        }
      } finally {
        recursionGuard = false
      }
    }

    fun compilationUnits(): Provider<List<SqlDelightCompilationUnitImpl>> = project.provider {
      compilationUnits.values.map { it.get() }
    }

    fun sourceFolders(source: Source): Provider<Set<SqlDelightSourceFolderImpl>> {
      val folderProvider = provideSourceFolders(source)

      compilationUnits[source.key] = project.provider {
        SqlDelightCompilationUnitImpl(
          name = source.name,
          sourceFolders = folderProvider.get(),
          outputDirectoryFile = source.outputDir,
        )
      }

      return folderProvider
    }

    private fun provideSourceFolders(source: Source): Provider<Set<SqlDelightSourceFolderImpl>> {
      val sourceFolders = source.sourceDirectories.map { dirs ->
        val declaredSet = srcDirs.mapTo(mutableSetOf()) { dir ->
          SqlDelightSourceFolderImpl(folder = dir, dependency = false)
        }

        if (declaredSet.isEmpty()) {
          dirs.mapTo(mutableSetOf()) { SqlDelightSourceFolderImpl(folder = it.asFile, dependency = false) }
        } else {
          declaredSet
        }
      }

      val dependencySet = project.provider {
        val resolvedArtifactResults = dependenciesProjects.get()
          .incoming
          .artifacts

        resolvedArtifactResults.fold(mutableMapOf(packageName.get() to mutableSetOf(project.path))) { acc, resolvedArtifactResult ->
          val latestVariant = resolvedArtifactResult.variant.latest()
          val packageName =
            latestVariant.attributes.getAttribute(SqlDelightPackageAttribute.ATTR)!!.name
          val components = acc.getOrPut(packageName) { mutableSetOf() }
          val component = latestVariant.owner as ProjectComponentIdentifier
          components.add(component.projectPath)
          acc[packageName] = components
          acc
        }
          .filter { entry ->
            entry.value.size > 1
          }
          .map { entry ->
            val projectsPaths = entry.value.joinToString(
              prefix = "[",
              postfix = "]",
            ) { projectPath ->
              "'$projectPath'"
            }
            "The package '${entry.key}' is defined in multiple projects $projectsPaths, which are used in the project '${project.path}'."
          }
          .joinToString(
            separator = "\n",
          )
          .takeIf { it.isNotEmpty() }
          ?.let { throw IllegalStateException(it) }

        resolvedArtifactResults
          .map {
            SqlDelightSourceFolderImpl(
              folder = File(project.projectDir, project.relativePath(it.file)),
              dependency = true,
            )
          }
      }
      return sourceFolders.zip(dependencySet) { files, dependencies -> files + dependencies }
    }

    private fun ResolvedVariantResult.latest(): ResolvedVariantResult {
      var variant: ResolvedVariantResult = this
      while (variant.externalVariant.isPresent) {
        variant = variant.externalVariant.get()
      }
      return variant
    }
  }
}
