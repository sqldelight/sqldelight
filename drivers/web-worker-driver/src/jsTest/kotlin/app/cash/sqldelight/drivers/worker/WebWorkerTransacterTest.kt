package app.cash.sqldelight.drivers.worker

import app.cash.sqldelight.SuspendingTransacter
import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail
import org.w3c.dom.Worker

class WebWorkerTransacterTest {
  private val schema = object : SqlSchema<QueryResult.Value<Unit>> {
    override val version = 1L
    override fun create(driver: SqlDriver) = QueryResult.Unit
    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }

  private fun runTest(block: suspend (SqlDriver, SuspendingTransacter) -> Unit) =
    kotlinx.coroutines.test.runTest {
      @Suppress("UnsafeCastFromDynamic")
      val driver = WebWorkerDriver(Worker(js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)""")))
        .also { schema.awaitCreate(it) }
      val transacter = object : SuspendingTransacterImpl(driver) {}
      block(driver, transacter)

      driver.close()
    }

  @Test fun afterCommit_runs_after_transaction_commits() = runTest { _, transacter ->
    var counter = 0
    transacter.transaction {
      afterCommit { counter++ }
      assertEquals(0, counter)
    }

    assertEquals(1, counter)
  }

  @Test fun afterCommit_does_not_run_after_transaction_rollbacks() = runTest { _, transacter ->
    var counter = 0
    transacter.transaction {
      afterCommit { counter++ }
      assertEquals(0, counter)
      rollback()
    }

    assertEquals(0, counter)
  }

  @Test fun afterCommit_runs_after_enclosing_transaction_commits() = runTest { _, transacter ->
    var counter = 0
    transacter.transaction {
      afterCommit { counter++ }
      assertEquals(0, counter)

      transactionWithResult {
        afterCommit { counter++ }
        assertEquals(0, counter)
      }

      assertEquals(0, counter)
    }

    assertEquals(2, counter)
  }

  @Test fun afterCommit_does_not_run_in_nested_transaction_when_enclosing_rolls_back() =
    runTest { _, transacter ->
      var counter = 0
      transacter.transaction {
        afterCommit { counter++ }
        assertEquals(0, counter)

        transactionWithResult {
          afterCommit { counter++ }
        }

        rollback()
      }

      assertEquals(0, counter)
    }

  @Test fun afterCommit_does_not_run_in_nested_transaction_when_nested_rolls_back() =
    runTest { _, transacter ->
      var counter = 0
      transacter.transaction {
        afterCommit { counter++ }
        assertEquals(0, counter)

        transactionWithResult {
          afterCommit { counter++ }
          rollback()
        }

        throw AssertionError()
      }

      assertEquals(0, counter)
    }

  @Test fun afterRollback_no_ops_if_the_transaction_never_rolls_back() = runTest { _, transacter ->
    var counter = 0
    transacter.transaction {
      afterRollback { counter++ }
    }

    assertEquals(0, counter)
  }

  @Test fun afterRollback_runs_after_a_rollback_occurs() = runTest { _, transacter ->
    var counter = 0
    transacter.transaction {
      afterRollback { counter++ }
      rollback()
    }

    assertEquals(1, counter)
  }

  @Test fun afterRollback_runs_after_an_inner_transaction_rolls_back() = runTest { _, transacter ->
    var counter = 0
    transacter.transaction {
      afterRollback { counter++ }
      transactionWithResult {
        rollback()
      }
      throw AssertionError()
    }

    assertEquals(1, counter)
  }

  @Test fun afterRollback_runs_in_an_inner_transaction_when_the_outer_transaction_rolls_back() =
    runTest { _, transacter ->
      var counter = 0
      transacter.transaction {
        transactionWithResult {
          afterRollback { counter++ }
        }
        rollback()
      }

      assertEquals(1, counter)
    }

  @Test fun transactions_close_themselves_out_properly() = runTest { _, transacter ->
    var counter = 0
    transacter.transaction {
      afterCommit { counter++ }
    }

    transacter.transaction {
      afterCommit { counter++ }
    }

    assertEquals(2, counter)
  }

  @Test fun setting_no_enclosing_fails_if_there_is_a_currently_running_transaction() =
    runTest { _, transacter ->
      transacter.transaction(noEnclosing = true) {
        assertFailsWith<IllegalStateException> {
          transacter.transaction(noEnclosing = true) {
            throw AssertionError()
          }
        }
      }
    }

  @Test
  fun an_exception_thrown_in_postRollback_function_is_combined_with_the_exception_in_the_main_body() =
    runTest { _, transacter ->
      class ExceptionA : RuntimeException()
      class ExceptionB : RuntimeException()
      try {
        transacter.transaction {
          afterRollback {
            throw ExceptionA()
          }
          throw ExceptionB()
        }
        fail("Should have thrown!")
      } catch (e: Throwable) {
        assertTrue("Exception thrown in body not in message($e)") {
          e.toString().contains("ExceptionA")
        }
        assertTrue("Exception thrown in rollback not in message($e)") {
          e.toString().contains("ExceptionB")
        }
      }
    }
}
