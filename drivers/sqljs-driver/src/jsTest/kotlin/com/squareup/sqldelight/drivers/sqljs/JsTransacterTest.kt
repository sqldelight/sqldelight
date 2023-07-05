package com.squareup.sqldelight.drivers.sqljs

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.sqljs.initSqlDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class JsTransacterTest {

  private val schema = object : SqlSchema<QueryResult.Value<Unit>> {
    override val version = 1L
    override fun create(driver: SqlDriver) = QueryResult.Unit
    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ) = QueryResult.Unit
  }

  private fun testing(action: suspend CoroutineScope.(SqlDriver, Transacter) -> Unit) = runTest {
    val driver = initSqlDriver().await()
    schema.create(driver)
    action(driver, object : TransacterImpl(driver) {})
    driver.close()
  }

  @Test fun afterCommit_runs_after_transaction_commits() = testing { _, transacter ->

    var counter = 0
    transacter.transaction {
      afterCommit { counter++ }
      assertEquals(0, counter)
    }

    assertEquals(1, counter)
  }

  @Test fun afterCommit_does_not_run_after_transaction_rollbacks() = testing { _, transacter ->

    var counter = 0
    transacter.transaction {
      afterCommit { counter++ }
      assertEquals(0, counter)
      rollback()
    }

    assertEquals(0, counter)
  }

  @Test fun afterCommit_runs_after_enclosing_transaction_commits() = testing { _, transacter ->

    var counter = 0
    transacter.transaction {
      afterCommit { counter++ }
      assertEquals(0, counter)

      transaction {
        afterCommit { counter++ }
        assertEquals(0, counter)
      }

      assertEquals(0, counter)
    }

    assertEquals(2, counter)
  }

  @Test fun afterCommit_does_not_run_in_nested_transaction_when_enclosing_rolls_back() = testing { _, transacter ->

    var counter = 0
    transacter.transaction {
      afterCommit { counter++ }
      assertEquals(0, counter)

      transaction {
        afterCommit { counter++ }
      }

      rollback()
    }

    assertEquals(0, counter)
  }

  @Test fun afterCommit_does_not_run_in_nested_transaction_when_nested_rolls_back() = testing { _, transacter ->

    var counter = 0
    transacter.transaction {
      afterCommit { counter++ }
      assertEquals(0, counter)

      transaction {
        afterCommit { counter++ }
        rollback()
      }

      throw AssertionError()
    }

    assertEquals(0, counter)
  }

  @Test fun afterRollback_no_ops_if_the_transaction_never_rolls_back() = testing { _, transacter ->

    var counter = 0
    transacter.transaction {
      afterRollback { counter++ }
    }

    assertEquals(0, counter)
  }

  @Test fun afterRollback_runs_after_a_rollback_occurs() = testing { _, transacter ->

    var counter = 0
    transacter.transaction {
      afterRollback { counter++ }
      rollback()
    }

    assertEquals(1, counter)
  }

  @Test fun afterRollback_runs_after_an_inner_transaction_rolls_back() = testing { _, transacter ->

    var counter = 0
    transacter.transaction {
      afterRollback { counter++ }
      transaction {
        rollback()
      }
      throw AssertionError()
    }

    assertEquals(1, counter)
  }

  @Test fun afterRollback_runs_in_an_inner_transaction_when_the_outer_transaction_rolls_back() = testing { _, transacter ->

    var counter = 0
    transacter.transaction {
      transaction {
        afterRollback { counter++ }
      }
      rollback()
    }

    assertEquals(1, counter)
  }

  @Test fun transactions_close_themselves_out_properly() = testing { _, transacter ->

    var counter = 0
    transacter.transaction {
      afterCommit { counter++ }
    }

    transacter.transaction {
      afterCommit { counter++ }
    }

    assertEquals(2, counter)
  }

  @Test fun setting_no_enclosing_fails_if_there_is_a_currently_running_transaction() = testing { _, transacter ->

    transacter.transaction(noEnclosing = true) {
      assertFailsWith<IllegalStateException> {
        transacter.transaction(noEnclosing = true) {
          throw AssertionError()
        }
      }
    }
  }

  @Test fun an_exception_thrown_in_postRollback_function_is_combined_with_the_exception_in_the_main_body() = testing { _, transacter ->
    class ExceptionA : RuntimeException()
    class ExceptionB : RuntimeException()
    val t = assertFailsWith<Throwable>() {
      transacter.transaction {
        afterRollback {
          throw ExceptionA()
        }
        throw ExceptionB()
      }
    }
    assertTrue("Exception thrown in body not in message($t)") { t.toString().contains("ExceptionA") }
    assertTrue("Exception thrown in rollback not in message($t)") { t.toString().contains("ExceptionB") }
  }
}
