package com.squareup.sqldelight.driver.test

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlDriver.Schema
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

abstract class TransacterTest {
  protected lateinit var transacter: TransacterImpl
  private lateinit var driver: SqlDriver

  abstract fun setupDatabase(schema: Schema): SqlDriver

  @BeforeTest fun setup() {
    val driver = setupDatabase(object : Schema {
      override val version = 1
      override fun create(driver: SqlDriver) {}
      override fun migrate(
        driver: SqlDriver,
        oldVersion: Int,
        newVersion: Int
      ) {
      }
    })
    transacter = object : TransacterImpl(driver) {}
    this.driver = driver
  }

  @AfterTest fun teardown() {
    driver.close()
  }

  @Test fun `afterCommit runs after transaction commits`() {
    var counter = 0
    transacter.transaction {
      afterCommit { counter++ }
      assertEquals(0, counter)
    }

    assertEquals(1, counter)
  }

  @Test fun `afterCommit does not run after transaction rollbacks`() {
    var counter = 0
    transacter.transaction {
      afterCommit { counter++ }
      assertEquals(0, counter)
      rollback()
    }

    assertEquals(0, counter)
  }

  @Test fun `afterCommit runs after enclosing transaction commits`() {
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

  @Test fun `afterCommit does not run in nested transaction when enclosing rolls back`() {
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

  @Test fun `afterCommit does not run in nested transaction when nested rolls back`() {
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

  @Test fun `afterRollback no-ops if the transaction never rolls back`() {
    var counter = 0
    transacter.transaction {
      afterRollback { counter++ }
    }

    assertEquals(0, counter)
  }

  @Test fun `afterRollback runs after a rollback occurs`() {
    var counter = 0
    transacter.transaction {
      afterRollback { counter++ }
      rollback()
    }

    assertEquals(1, counter)
  }

  @Test fun `afterRollback runs after an inner transaction rolls back`() {
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

  @Test fun `afterRollback runs in an inner transaction when the outer transaction rolls back`() {
    var counter = 0
    transacter.transaction {
      transaction {
        afterRollback { counter++ }
      }
      rollback()
    }

    assertEquals(1, counter)
  }

  @Test fun `transactions close themselves out properly`() {
    var counter = 0
    transacter.transaction {
      afterCommit { counter++ }
    }

    transacter.transaction {
      afterCommit { counter++ }
    }

    assertEquals(2, counter)
  }

  @Test fun `setting no enclosing fails if there is a currently running transaction`() {
    transacter.transaction(noEnclosing = true) {
      try {
        transacter.transaction(noEnclosing = true) {
          throw AssertionError()
        }
        throw AssertionError()
      } catch (e: IllegalStateException) {
        // Expected error.
      }
    }
  }

  @Test
  fun `An exception thrown in postRollback function is combined with the exception in the main body`() {
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
      assertTrue("Exception thrown in body not in message($e)") { e.toString().contains("ExceptionA") }
      assertTrue("Exception thrown in rollback not in message($e)") { e.toString().contains("ExceptionB") }
    }
  }

  @Test
  fun `we can return a value from a transaction`() {
    val result: String = transacter.transactionWithResult {
      return@transactionWithResult "sup"
    }

    assertEquals(result, "sup")
  }

  @Test
  fun `we can rollback with value from a transaction`() {
    val result: String = transacter.transactionWithResult {
      rollback("rollback")

      @Suppress("UNREACHABLE_CODE")
      return@transactionWithResult "sup"
    }

    assertEquals(result, "rollback")
  }
}
