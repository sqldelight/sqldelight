package app.cash.sqldelight.drivers.worker

import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitQuery
import app.cash.sqldelight.driver.worker.WebWorkerException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runTest

class WorkerLifecycleTest {
  @Test
  fun close_and_await_fails_pending_requests_and_rejects_new_requests() = runTest {
    val driver = createControllableWebWorkerDriver()

    supervisorScope {
      val first = async(start = CoroutineStart.UNDISPATCHED) {
        driver.await(null, "hold", 0)
      }
      val second = async(start = CoroutineStart.UNDISPATCHED) {
        driver.await(null, "hold", 0)
      }

      driver.closeAndAwait()

      assertFailsWith<WebWorkerException> { first.await() }
      assertFailsWith<WebWorkerException> { second.await() }
    }
    assertFailsWith<WebWorkerException> {
      driver.await(null, "after-close", 0)
    }

    driver.close()
    driver.closeAndAwait()
  }

  @Test
  fun worker_error_is_terminal_for_pending_and_subsequent_requests() = runTest {
    val driver = createControllableWebWorkerDriver()

    supervisorScope {
      val held = async(start = CoroutineStart.UNDISPATCHED) {
        driver.await(null, "hold", 0)
      }
      val failing = async(start = CoroutineStart.UNDISPATCHED) {
        driver.await(null, "worker-error", 0)
      }

      val failure = assertFailsWith<WebWorkerException> { failing.await() }
      assertContains(failure.message.orEmpty(), "terminal worker failure")
      val heldFailure = assertFailsWith<WebWorkerException> { held.await() }
      assertContains(heldFailure.message.orEmpty(), "terminal worker failure")
    }

    val subsequentFailure = assertFailsWith<WebWorkerException> {
      driver.await(null, "after-error", 0)
    }
    assertContains(subsequentFailure.message.orEmpty(), "terminal worker failure")
    driver.close()
  }

  @Test
  fun failed_transaction_messages_restore_local_state() = runTest {
    val driver = createControllableWebWorkerDriver()
    val transacter = object : SuspendingTransacterImpl(driver) {}
    try {
      driver.await(null, "fail-next-begin", 0)
      assertFailsWith<WebWorkerException> {
        driver.newTransaction().await()
      }
      assertNull(driver.currentTransaction())

      transacter.transaction {}
      assertNull(driver.currentTransaction())

      assertFailsWith<WebWorkerException> {
        transacter.transaction {
          driver.await(null, "fail-next-end", 0)
        }
      }
      assertNull(driver.currentTransaction())

      assertFailsWith<WebWorkerException> {
        transacter.transaction {
          driver.await(null, "fail-next-rollback", 0)
          rollback()
        }
      }
      assertNull(driver.currentTransaction())
    } finally {
      driver.close()
    }
  }

  @Test
  fun boolean_and_numeric_worker_values_are_supported() = runTest {
    val driver = createControllableWebWorkerDriver()
    try {
      driver.awaitQuery(
        identifier = null,
        sql = "booleans",
        mapper = { cursor ->
          cursor.next().await()
          assertEquals(true, cursor.getBoolean(0))
          assertEquals(false, cursor.getBoolean(1))
          assertEquals(true, cursor.getBoolean(2))
          assertEquals(false, cursor.getBoolean(3))
        },
        parameters = 0,
      )
    } finally {
      driver.close()
    }
  }
}
