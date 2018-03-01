package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.sqlite.jdbc.SqliteJdbcOpenHelper
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class TransacterTest {
  private lateinit var transacter: Transacter
  private lateinit var connection: SqlDatabaseConnection
  private lateinit var databaseHelper: SqliteJdbcOpenHelper

  @Before fun setup() {
    databaseHelper = SqliteJdbcOpenHelper()
    transacter = object : Transacter(databaseHelper) {}
    connection = databaseHelper.getConnection()
  }

  @After fun teardown() {
    databaseHelper.close()
  }

  @Test fun `afterCommit runs after transaction commits`() {
    val counter = AtomicInteger(0)
    transacter.transaction {
      afterCommit { counter.incrementAndGet() }
      assertThat(counter.get()).isEqualTo(0)
    }

    assertThat(counter.get()).isEqualTo(1)
  }

  @Test fun `afterCommit does not run after transaction rollbacks`() {
    val counter = AtomicInteger(0)
    transacter.transaction {
      afterCommit { counter.incrementAndGet() }
      assertThat(counter.get()).isEqualTo(0)
      rollback()
    }

    assertThat(counter.get()).isEqualTo(0)
  }

  @Test fun `deferAction runs immediately with no transaction`() {
    val counter = AtomicInteger(0)
    transacter.deferAction { counter.incrementAndGet() }
    assertThat(counter.get()).isEqualTo(1)
  }

  @Test fun `afterCommit runs after enclosing transaction commits`() {
    val counter = AtomicInteger(0)
    transacter.transaction {
      afterCommit { counter.incrementAndGet() }
      assertThat(counter.get()).isEqualTo(0)

      transaction {
        afterCommit { counter.incrementAndGet() }
        assertThat(counter.get()).isEqualTo(0)
      }

      assertThat(counter.get()).isEqualTo(0)
    }

    assertThat(counter.get()).isEqualTo(2)
  }

  @Test fun `afterCommit does not run in nested transaction when enclosing rolls back`() {
    val counter = AtomicInteger(0)
    transacter.transaction {
      afterCommit { counter.incrementAndGet() }
      assertThat(counter.get()).isEqualTo(0)

      transaction {
        afterCommit { counter.incrementAndGet() }
      }

      rollback()
    }

    assertThat(counter.get()).isEqualTo(0)
  }

  @Test fun `afterCommit does not run in nested transaction when nested rolls back`() {
    val counter = AtomicInteger(0)
    transacter.transaction {
      afterCommit { counter.incrementAndGet() }
      assertThat(counter.get()).isEqualTo(0)

      transaction {
        afterCommit { counter.incrementAndGet() }
        rollback()
      }

      throw AssertionError()
    }

    assertThat(counter.get()).isEqualTo(0)
  }

  @Test fun `afterRollback no-ops if the transaction never rolls back`() {
    val counter = AtomicInteger(0)
    transacter.transaction {
      afterRollback { counter.incrementAndGet() }
    }

    assertThat(counter.get()).isEqualTo(0)
  }

  @Test fun `afterRollback runs after a rollback occurs`() {
    val counter = AtomicInteger(0)
    transacter.transaction {
      afterRollback { counter.incrementAndGet() }
      rollback()
    }

    assertThat(counter.get()).isEqualTo(1)
  }

  @Test fun `afterRollback runs after an inner transaction rolls back`() {
    val counter = AtomicInteger(0)
    transacter.transaction {
      afterRollback { counter.incrementAndGet() }
      transaction {
        rollback()
      }
      throw AssertionError()
    }

    assertThat(counter.get()).isEqualTo(1)
  }

  @Test fun `afterRollback runs in an inner transaction when the outer transaction rolls back`() {
    val counter = AtomicInteger(0)
    transacter.transaction {
      transaction {
        afterRollback { counter.incrementAndGet() }
      }
      rollback()
    }

    assertThat(counter.get()).isEqualTo(1)
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

  @Ignore @Test fun `An exception thrown in postRollback function is combined with the exception in the main body`() {
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
