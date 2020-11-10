package com.squareup.sqldelight.gradle

import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

/**
 * Common API for interacting with gradle workers
 * in tasks
 */
interface SqlDelightWorkerTask {

  val workerExecutor: WorkerExecutor

  /**
   * If true, [WorkerExecutor.classLoaderIsolation] is used. Otherwise,
   * [WorkerExecutor.noIsolation] is used for running the task's action
   */
  var useClassLoaderIsolation: Boolean

  val workQueue: WorkQueue
    get() = if (useClassLoaderIsolation) {
      workerExecutor.classLoaderIsolation()
    } else {
      workerExecutor.noIsolation()
    }
}
