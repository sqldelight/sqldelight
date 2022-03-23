package app.cash.sqldelight.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SourceTask
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Common API for interacting with gradle workers
 * in tasks
 */
abstract class SqlDelightWorkerTask : SourceTask() {

  @get:Inject
  internal abstract val workerExecutor: WorkerExecutor

  @get:[InputFiles Classpath]
  internal val classpath: ConfigurableFileCollection = project.objects.fileCollection()

  internal fun workQueue(): WorkQueue =
    workerExecutor.classLoaderIsolation {
      it.classpath.from(classpath)
    }
}
