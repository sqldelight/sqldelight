package com.squareup.sqldelight.driver

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class TransacterTest {
  private lateinit var transacter: Transacter
  private lateinit var databaseHelper: SqlDatabase

  @BeforeTest fun setup() {
    databaseHelper = setupDatabase(object : SqlDatabase.Schema {
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
  }

  @AfterTest fun teardown() {
    databaseHelper.close()
  }

  abstract fun setupDatabase(schema: SqlDatabase.Schema): SqlDatabase

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
