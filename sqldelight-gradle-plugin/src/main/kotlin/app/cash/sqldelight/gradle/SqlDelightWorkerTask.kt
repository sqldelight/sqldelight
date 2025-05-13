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
    }
}
