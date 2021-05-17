package com.squareup.sqldelight.runtime.rx3

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable.INSTANCE
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.TimeUnit

/**
 * A simplified test scheduler that doesn't check if the worker is disposed before triggering
 * actions. This allows reproducing race conditions where the worker becomes disposed in between
 * checking that its disposed and running the action.
 */
class NeverDisposedTestScheduler : Scheduler() {

  private val queue: Queue<Runnable> = LinkedList()

  fun triggerActions() {
    while (true) {
      val current = queue.peek() ?: return
      queue.remove(current)
      current.run()
    }
  }

  override fun createWorker(): Worker {
    return TestWorker()
  }

  private inner class TestWorker : Worker() {
    private var disposed = false

    override fun dispose() {
      disposed = true
    }

    override fun isDisposed() = disposed

    override fun schedule(run: Runnable, delayTime: Long, unit: TimeUnit): Disposable {
      return if (disposed) {
        INSTANCE
      } else {
        queue.add(run)
        Disposable.fromRunnable {
          queue.remove(run)
        }
      }
    }
  }
}