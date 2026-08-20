package app.cash.sqldelight.gradle

import javax.inject.Inject
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
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
      forkOptions.jvmArgs(unsafeMemoryAccessJvmArgs(Runtime.version().feature()))
    }
  }
}

/**
 * Suppress "WARNING: A terminally deprecated method in sun.misc.Unsafe has been called".
 * The IntelliJ platform runs on calls to terminally deprecated `sun.misc.Unsafe` memory access methods,
 * which JEP 498 warns about and eventually denies, in phases.
 */
internal fun unsafeMemoryAccessJvmArgs(javaFeatureVersion: Int): List<String> {
  return when {
    javaFeatureVersion < 23 -> emptyList()
    javaFeatureVersion < 26 -> listOf("--sun-misc-unsafe-memory-access=allow")
    else -> listOf("--sun-misc-unsafe-memory-access=warn")
  }
}
