package app.cash.sqldelight.gradle

import javax.inject.Inject
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.SourceTask
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

/**
 * Common API for interacting with gradle workers
 * in tasks
 */
@CacheableTask
abstract class SqlDelightWorkerTask : SourceTask() {

  @get:Inject
  internal abstract val workerExecutor: WorkerExecutor

  @get:Classpath
  abstract val classpath: ConfigurableFileCollection

  internal fun workQueue(): WorkQueue =
    workerExecutor.processIsolation {
      it.classpath.from(classpath)

      it.forkOptions { forkOptions ->
        // Environment variables are not forwarded to worker processes by default,
        // see https://github.com/gradle/gradle/issues/8030.
        // This breaks retrieval of environment variables like temporary directories in the worker
        // process, which are required for JDBC to work correctly.
        // Therefore, we explicitly forward all environment variables to the worker process.
        forkOptions.environment(System.getenv())

        // A heap size of 2G is reasonable (determined experimentally)
        forkOptions.jvmArgs("-Xmx2G")
      }
    }
}
