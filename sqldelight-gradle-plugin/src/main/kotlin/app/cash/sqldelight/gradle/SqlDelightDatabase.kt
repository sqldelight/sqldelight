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
import kotlin.DeprecationLevel.HIDDEN
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
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

  private val sources by lazy { sources(project) }

  fun methodMissing(name: String, args: Any): Any {
    return (project as GroovyObject).invokeMethod(name, args)
  }

  // Declarable bucket for schema dependencies.
  // Replace `register` with `dependencyScope` when min Gradle version is >= 8.4.
  private val schemaImplementation = project.configurations.register("schema${name}Implementation") {
    it.isCanBeConsumed = false
    it.isCanBeResolved = false
  }

  @Suppress("unused") // Public API used in gradle files.
  @Deprecated("use ProjectDependency", level = HIDDEN)
  fun dependency(delegatedProject: DelegatingProjectDependency) = dependency(project.project(delegatedProject.path))

  @Suppress("unused") // Public API used in gradle files.
  fun dependency(delegatedProject: ProjectDependency) = dependency(project.project(delegatedProject.path))

  @Suppress("unused") // Public API used in gradle files.
  fun dependency(dependencyProject: Project) {
    schemaImplementation.configure {
      it.dependencies.add(project.dependencies.create(dependencyProject))
    }
  }

  fun srcDirs(vararg srcPaths: Any) {
    srcDirs.from(srcPaths)
  }

  // This causes eager resolution and should only be called by the ToolingModelBuilder.
  internal fun resolveProperties(): SqlDelightDatabasePropertiesImpl {
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

    fun sourceFolders(source: Source): Set<SqlDelightSourceFolderImpl> {
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

      val dependencySourceFolders = project.configurations.findByName(source.schemaClasspathName)
        ?.incoming
        ?.artifacts
        ?.map { artifact ->
          SqlDelightSourceFolderImpl(
            folder = File(project.projectDir, project.relativePath(artifact.file.absolutePath)),
            dependency = true,
          )
        }.orEmpty()

      return sourceFolders + dependencySourceFolders
    }

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
      dependencies = sources.flatMap { source ->
        project.configurations.findByName(source.schemaClasspathName)
          ?.incoming
          ?.artifacts
          ?.map { artifact ->
            val artifactPackageName = requireNotNull(artifact.variant.attributes.getAttribute(PackageNameAttribute)) {
              "Dependency missing packageName attribute for ${artifact.id.displayName}"
            }
            val databaseName = requireNotNull(artifact.variant.attributes.getAttribute(DatabaseNameAttribute)) {
              "Dependency missing database name attribute for ${artifact.id.displayName}"
            }

            check(packageName.get() != artifactPackageName) {
              "Detected a schema that already has the package name $artifactPackageName"
            }

            SqlDelightDatabaseNameImpl(
              packageName = artifactPackageName,
              className = databaseName,
            )
          }.orEmpty()
      },
      deriveSchemaFromMigrations = deriveSchemaFromMigrations.get(),
      treatNullAsUnknownForEquality = treatNullAsUnknownForEquality.get(),
      generateAsync = generateAsync.get(),
    )
  }

  // Replace `register` with `consumable` when min Gradle version is >= 8.4.
  private fun publishSchemaArtifacts(
    source: Source,
    localSourceDirs: Provider<List<File>>,
  ) {
    project
      .configurations
      .register(source.schemaElementsName) {
        it.isCanBeConsumed = true
        it.isCanBeResolved = false

        it.extendsFrom(schemaImplementation.get())
        it.attributes.apply {
          attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, "sqldelight-schema"))
          attribute(DatabaseNameAttribute, this@SqlDelightDatabase.name)
          attribute(SourceNameAttribute, source.name)
          attributeProvider(PackageNameAttribute, packageName)
        }

        it.outgoing.artifacts(localSourceDirs)
      }
  }

  // Replace `register` with `resolvable` when min Gradle version is >= 8.4.
  private fun registerResolvableSchema(
    source: Source,
  ): NamedDomainObjectProvider<Configuration> = project
    .configurations
    .register(source.schemaClasspathName) {
      it.isCanBeConsumed = false
      it.isCanBeResolved = true

      it.extendsFrom(schemaImplementation.get())
      it.attributes.apply {
        attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, "sqldelight-schema"))
        attribute(DatabaseNameAttribute, this@SqlDelightDatabase.name)
        attribute(SourceNameAttribute, source.name)
      }
    }

  internal fun registerTasks() {
    sources.forEach { source ->
      // Set here to avoid capturing `project` in the map provider.
      val sourceSetDirs = source.sourceSets.map { project.file("src/$it/sqldelight") }
      val localSourceDirs = srcDirs.elements.map { userSrcDirs ->
        when {
          userSrcDirs.isEmpty() -> sourceSetDirs
          else -> userSrcDirs.map { it.asFile }
        }
      }

      publishSchemaArtifacts(source, localSourceDirs)
      val schemaClasspath = registerResolvableSchema(source)

      // Register the sqldelight generating task.
      val task = project.tasks.register("generate${source.name.capitalize()}${name}Interface", SqlDelightTask::class.java) {
        it.projectName.set(project.name)
        it.setCommonProperties(schemaClasspath, localSourceDirs, source)
        it.outputDirectory.set(source.outputDir)
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
        addMigrationTasks(schemaClasspath, localSourceDirs, source)
      }

      if (deriveSchemaFromMigrations.get()) {
        addSquashTask(schemaClasspath, localSourceDirs, source)
      }

      if (migrationOutputDirectory.isPresent) {
        addMigrationOutputTasks(schemaClasspath, localSourceDirs, source)
      }
    }
  }

  private fun addMigrationTasks(
    schemaClasspath: NamedDomainObjectProvider<Configuration>,
    localSourceDirs: Provider<List<File>>,
    source: Source,
  ) {
    val verifyMigrationTask =
      project.tasks.register("verify${source.name.capitalize()}${name}Migration", VerifyMigrationTask::class.java) {
        it.projectName.set(project.name)
        it.setCommonProperties(schemaClasspath, localSourceDirs, source)
        it.include("**${File.separatorChar}*.$SQLDELIGHT_EXTENSION")
        it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
        it.workingDirectory.set(File(project.buildDir, "sqldelight/migration_verification/${source.name.capitalize()}$name"))
        it.group = SqlDelightPlugin.GROUP
        it.description = "Verify ${source.name} $name migrations and CREATE statements match."
        it.verifyMigrations.set(verifyMigrations)
        it.verifyDefinitions.set(verifyDefinitions)
        it.classpath.setFrom(intellijEnv, migrationEnv, configuration)
      }

    if (schemaOutputDirectory.isPresent) {
      project.tasks.register("generate${source.name.capitalize()}${name}Schema", GenerateSchemaTask::class.java) {
        it.projectName.set(project.name)
        it.setCommonProperties(schemaClasspath, localSourceDirs, source)
        it.outputDirectory.set(schemaOutputDirectory)
        it.include("**${File.separatorChar}*.$SQLDELIGHT_EXTENSION")
        it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
        it.group = SqlDelightPlugin.GROUP
        it.description = "Generate a .db file containing the current $name schema for ${source.name}."
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
    schemaClasspath: NamedDomainObjectProvider<Configuration>,
    localSourceDirs: Provider<List<File>>,
    source: Source,
  ) {
    project.tasks.register("generate${source.name.capitalize()}${name}Migrations", GenerateMigrationOutputTask::class.java) {
      it.projectName.set(project.name)
      it.setCommonProperties(schemaClasspath, localSourceDirs, source)
      it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
      it.migrationOutputExtension.set(migrationOutputFileFormat)
      it.outputDirectory.set(migrationOutputDirectory)
      it.group = SqlDelightPlugin.GROUP
      it.description = "Generate valid sql migration files for ${source.name} $name."
      it.classpath.setFrom(intellijEnv, configuration)
    }
  }

  private fun addSquashTask(
    schemaClasspath: NamedDomainObjectProvider<Configuration>,
    localSourceDirs: Provider<List<File>>,
    source: Source,
  ) {
    project.tasks.register("squash${source.name.capitalize()}${name}Migrations", MigrationSquashTask::class.java) {
      it.projectName.set(project.name)
      it.setCommonProperties(schemaClasspath, localSourceDirs, source)
      it.include("**${File.separatorChar}*.$MIGRATION_EXTENSION")
      it.group = SqlDelightPlugin.GROUP
      it.description = "Squash migrations into a single file for ${source.name} $name."
      it.classpath.setFrom(intellijEnv, configuration)
    }
  }

  private fun SqlDelightWorkerTask.setCommonProperties(
    schemaClasspath: NamedDomainObjectProvider<Configuration>,
    localSourceDirs: Provider<List<File>>,
    source: Source,
  ) {
    this.packageName.set(this@SqlDelightDatabase.packageName)
    this.className.set(this@SqlDelightDatabase.name)
    this.sourceName.set(source.name)
    this.deriveSchemaFromMigrations.set(this@SqlDelightDatabase.deriveSchemaFromMigrations)
    this.treatNullAsUnknownForEquality.set(this@SqlDelightDatabase.treatNullAsUnknownForEquality)
    this.generateAsync.set(this@SqlDelightDatabase.generateAsync)
    this.rootDirectory.set(project.projectDir)
    this.schemaOutputDirectory.set(source.outputDir)
    this.localSourceDirs.from(localSourceDirs)
    dependenciesFrom(schemaClasspath.flatMap { config -> config.incoming.artifacts.resolvedArtifacts })
  }

  private val Source.outputDir get() = File(generatedSourcesDirectory, name)

  private val Source.schemaClasspathName get() = "schema${this.name.capitalize()}${this@SqlDelightDatabase.name}Classpath"
  private val Source.schemaElementsName get() = "schema${this.name.capitalize()}${this@SqlDelightDatabase.name}Elements"

  internal companion object {
    val PackageNameAttribute: Attribute<String> = Attribute.of("app.cash.sqldelight.packageName", String::class.java)
    val DatabaseNameAttribute: Attribute<String> = Attribute.of("app.cash.sqldelight.database", String::class.java)
    val SourceNameAttribute: Attribute<String> = Attribute.of("app.cash.sqldelight.source", String::class.java)
  }
}
