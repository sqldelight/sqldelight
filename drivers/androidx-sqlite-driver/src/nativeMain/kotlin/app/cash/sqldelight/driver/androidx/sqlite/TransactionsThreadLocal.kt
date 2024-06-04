package app.cash.sqldelight.driver.androidx.sqlite

import app.cash.sqldelight.Transacter
import kotlin.native.concurrent.Worker
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

private class WorkerLocal {
  val workerData = atomic<MutableMap<Worker, Transacter.Transaction?>>(mutableMapOf())
}

internal actual class TransactionsThreadLocal actual constructor() {
  private val workerLocal = WorkerLocal()

  actual fun get(): Transacter.Transaction? {
    val worker = Worker.current
    return workerLocal.workerData?.value?.get(worker)
  }

  actual fun set(transaction: Transacter.Transaction?) {
    val worker = Worker.current
    workerLocal.workerData.update { data ->
      if (transaction == null) {
        data.remove(worker)
      } else {
        data[worker] = transaction
      }
      data
    }
  }
}
