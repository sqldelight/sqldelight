package app.cash.sqldelight.gradle

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Input
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

  /**
   * If true, [WorkerExecutor.classLoaderIsolation] is used. Otherwise,
   * [WorkerExecutor.noIsolation] is used for running the task's action
   */
  @get:Input
  internal var useClassLoaderIsolation: Boolean = true

  internal fun workQueue(): WorkQueue = if (useClassLoaderIsolation) {
    workerExecutor.classLoaderIsolation()
  } else {
    workerExecutor.noIsolation()
  }

  /**
   * Makes the task use [WorkerExecutor.noIsolation]
   * instead of [WorkerExecutor.classLoaderIsolation]
   */
  @Suppress("unused")
  fun disableClassLoaderIsolation() {
    useClassLoaderIsolation = false
    usesService(
      project.gradle.sharedServices.registerIfAbsent(
        SqlDelightWorkerTaskSerialService::class.toString(),
        SqlDelightWorkerTaskSerialService::class.java
      ) { it.maxParallelUsages.set(1) }
    )
  }
}

private abstract class SqlDelightWorkerTaskSerialService : BuildService<BuildServiceParameters.None>
