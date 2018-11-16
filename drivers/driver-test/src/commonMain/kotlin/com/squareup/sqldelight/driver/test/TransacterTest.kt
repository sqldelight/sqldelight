package com.squareup.sqldelight.driver.test

import co.touchlab.stately.concurrency.AtomicInt
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabase.Schema
import com.squareup.sqldelight.db.SqlDatabaseConnection
import kotlin.test.*

abstract class TransacterTest {
  private lateinit var transacter: Transacter
  private lateinit var databaseHelper: SqlDatabase

  abstract fun setupDatabase(schema: Schema): SqlDatabase

  @BeforeTest fun setup() {
    val databaseHelper = setupDatabase(object : Schema {
      override val version = 1
      override fun create(db: SqlDatabaseConnection) {}
      override fun migrate(
        db: SqlDatabaseConnection,
        oldVersion: Int,
        newVersion: Int
      ) {
      }
    })
    transacter = object : Transacter(databaseHelper) {}
    this.databaseHelper = databaseHelper
  }

  @AfterTest fun teardown() {
    databaseHelper.close()
  }

  @Test fun `afterCommit runs after transaction commits`() {
    val counter = AtomicInt(0)
    transacter.transaction {
      afterCommit { counter.increment() }
      assertEquals(0, counter.value)
    }

    assertEquals(1, counter.value)
  }

  @Test fun `afterCommit does not run after transaction rollbacks`() {
    val counter = AtomicInt(0)
    transacter.transaction {
      afterCommit { counter.increment() }
      assertEquals(0, counter.value)
      rollback()
    }

    assertEquals(0, counter.value)
  }

  @Test fun `afterCommit runs after enclosing transaction commits`() {
    val counter = AtomicInt(0)
    transacter.transaction {
      afterCommit { counter.increment() }
      assertEquals(0, counter.value)

      transaction {
        afterCommit { counter.increment() }
        assertEquals(0, counter.value)
      }

      assertEquals(0, counter.value)
    }

    assertEquals(2, counter.value)
  }

  @Test fun `afterCommit does not run in nested transaction when enclosing rolls back`() {
    val counter = AtomicInt(0)
    transacter.transaction {
      afterCommit { counter.increment() }
      assertEquals(0, counter.value)

      transaction {
        afterCommit { counter.increment() }
      }

      rollback()
    }

    assertEquals(0, counter.value)
  }

  @Test fun `afterCommit does not run in nested transaction when nested rolls back`() {
    val counter = AtomicInt(0)
    transacter.transaction {
      afterCommit { counter.increment() }
      assertEquals(0, counter.value)

      transaction {
        afterCommit { counter.increment() }
        rollback()
      }

      throw AssertionError()
    }

    assertEquals(0, counter.value)
  }

  @Test fun `afterRollback no-ops if the transaction never rolls back`() {
    val counter = AtomicInt(0)
    transacter.transaction {
      afterRollback { counter.increment() }
    }

    assertEquals(0, counter.value)
  }

  @Test fun `afterRollback runs after a rollback occurs`() {
    val counter = AtomicInt(0)
    transacter.transaction {
      afterRollback { counter.increment() }
      rollback()
    }

    assertEquals(1, counter.value)
  }

  @Test fun `afterRollback runs after an inner transaction rolls back`() {
    val counter = AtomicInt(0)
    transacter.transaction {
      afterRollback { counter.increment() }
      transaction {
        rollback()
      }
      throw AssertionError()
    }

    assertEquals(1, counter.value)
  }

  @Test fun `afterRollback runs in an inner transaction when the outer transaction rolls back`() {
    val counter = AtomicInt(0)
    transacter.transaction {
      transaction {
        afterRollback { counter.increment() }
      }
      rollback()
    }

    assertEquals(1, counter.value)
  }

  @Test fun `transactions close themselves out properly`() {
    val counter = AtomicInt(0)
    transacter.transaction {
      afterCommit {
        counter.increment()
      }
    }

    transacter.transaction {
      afterCommit {
        counter.increment()
      }
    }

    assertEquals(2, counter.value)
  }

  @Test fun `setting no enclosing fails if there is a currently running transaction`() {
    transacter.transaction(noEnclosing = true) {
      try {
        this@TransacterTest.transacter.transaction(noEnclosing = true) {
          throw AssertionError()
        }
        throw AssertionError()
      } catch (e: IllegalStateException) {
        // Expected error.
      }
    }
  }

  @Ignore @Test
  fun `An exception thrown in postRollback function is combined with the exception in the main body`() {
    try {
      transacter.transaction {
        afterRollback {
          throw RuntimeException("afterRollback exception")
        }
        throw RuntimeException("transaction exception")
      }
    } catch (e: RuntimeException) {
      // Verify it is a composite exception with both exceptions printed in the stack trace.
    }
  }
}
