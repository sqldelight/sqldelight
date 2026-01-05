package app.cash.sqldelight.gradle

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.gradle.SqlDelightDatabase.Companion.DatabaseNameAttribute
import app.cash.sqldelight.gradle.SqlDelightDatabase.Companion.PackageNameAttribute
import javax.inject.Inject
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceTask
import org.gradle.process.JavaForkOptions
import org.gradle.workers.ClassLoaderWorkerSpec
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

/**
 * Common API for interacting with gradle workers in tasks
 */
@CacheableTask
abstract class SqlDelightWorkerTask : SourceTask() {

  @get:Inject
  internal abstract val workerExecutor: WorkerExecutor

  /** @see ClassLoaderWorkerSpec.getClasspath */
  @get:Classpath
  abstract val classpath: ConfigurableFileCollection

  @get:Input
  abstract val packageName: Property<String>

  @get:Input
  abstract val className: Property<String>

  @get:Input
  abstract val sourceName: Property<String>

  @get:Input
  abstract val deriveSchemaFromMigrations: Property<Boolean>

  @get:Input
  abstract val treatNullAsUnknownForEquality: Property<Boolean>

  @get:Input
  abstract val generateAsync: Property<Boolean>

  @get:Input
  abstract val dependencies: ListProperty<SqlDelightDatabaseNameImpl>

  @get:Internal
  abstract val rootDirectory: DirectoryProperty

  @get:Internal
  abstract val schemaOutputDirectory: DirectoryProperty

  // Internal since it is added to source.
  @get:Internal
  abstract val localSourceDirs: ConfigurableFileCollection

  // Internal since it is added to source.
  @get:Internal
  abstract val dependencySourceDirs: ConfigurableFileCollection

  init {
    deriveSchemaFromMigrations.convention(false)
    treatNullAsUnknownForEquality.convention(false)
    generateAsync.convention(false)

    source(localSourceDirs)
    source(dependencySourceDirs)
  }

  // The metadata and files have to be split into separate inputs to maintain cache compatibility.
  // See https://docs.gradle.org/current/userguide/artifact_resolution.html#resolving_artifacts
  internal fun dependenciesFrom(artifacts: Provider<Set<ResolvedArtifactResult>>) {
    dependencies.set(
      artifacts.map { resolvedArtifacts ->
        resolvedArtifacts.map { artifact ->
          val packageName = requireNotNull(artifact.variant.attributes.getAttribute(PackageNameAttribute)) {
            "Dependency missing packageName attribute for ${artifact.id.displayName}"
          }
          val databaseName = requireNotNull(artifact.variant.attributes.getAttribute(DatabaseNameAttribute)) {
            "Dependency missing database name  attribute for ${artifact.id.displayName}"
          }
          SqlDelightDatabaseNameImpl(
            packageName = packageName,
            className = databaseName,
          )
        }
      },
    )

    dependencySourceDirs.from(
      artifacts.map { resolvedArtifacts ->
        resolvedArtifacts.map { it.file }
      },
    )
  }

  internal fun resolveProperties(): SqlDelightDatabaseProperties = SqlDelightDatabasePropertiesImpl(
    packageName = packageName.get(),
    className = className.get(),
    dependencies = dependencies.get(),
    rootDirectory = rootDirectory.get().asFile,
    deriveSchemaFromMigrations = deriveSchemaFromMigrations.get(),
    treatNullAsUnknownForEquality = treatNullAsUnknownForEquality.get(),
    generateAsync = generateAsync.get(),
  )

  internal fun resolveCompilationUnit(outputDirectory: DirectoryProperty): SqlDelightCompilationUnit {
    val sourceFolders = buildSet {
      localSourceDirs.forEach { add(SqlDelightSourceFolderImpl(it, false)) }
      dependencySourceDirs.forEach { add(SqlDelightSourceFolderImpl(it, true)) }
    }

    return SqlDelightCompilationUnitImpl(
      name = sourceName.get(),
      sourceFolders = sourceFolders,
      outputDirectoryFile = outputDirectory.get().asFile,
    )
  }

  /** @see JavaForkOptions.getMinHeapSize */
  @get:Internal
  val minHeapSize: Property<String> =
    project.objects.property(String::class.java)

  /** @see JavaForkOptions.getMaxHeapSize */
  @get:Internal
  val maxHeapSize: Property<String> =
    project.objects.property(String::class.java).convention("512M")

  internal fun workQueue(): WorkQueue = workerExecutor.processIsolation { workerSpec ->
    workerSpec.classpath.from(classpath)

    workerSpec.forkOptions { forkOptions ->
      forkOptions.defaultCharacterEncoding = "UTF-8"
      // Necessary for SQLiteJDBCLoader and SQLiteConnection, otherwise Windows will default to the system root.
      val tmpdir = System.getProperty("java.io.tmpdir")
      forkOptions.environment("TMP", tmpdir)
      forkOptions.environment("TMPDIR", tmpdir)
      forkOptions.minHeapSize = minHeapSize.orNull
      forkOptions.maxHeapSize = maxHeapSize.get()
    }
  }
}
