package app.cash.sqldelight.driver.androidx.sqlite

import app.cash.sqldelight.TransactionWithReturn
import app.cash.sqldelight.TransactionWithoutReturn
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.test.Test

class AndroidxSqliteTransacterTest : CommonTransacterTest() {
  override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    return AndroidxSqliteDriver(androidxSqliteTestDriver(), name = null, schema)
  }

  @Test fun `detect the afterRollback call has escaped the original transaction thread in transaction`() {
    assertChecksThreadConfinement<TransactionWithoutReturn>(
      transacter = transacter,
      scope = { transaction(false, it) },
      block = { afterRollback {} },
    )
  }

  @Test fun `detect the afterCommit call has escaped the original transaction thread in transaction`() {
    assertChecksThreadConfinement<TransactionWithoutReturn>(
      transacter = transacter,
      scope = { transaction(false, it) },
      block = { afterCommit {} },
    )
  }

  @Test fun `detect the rollback call has escaped the original transaction thread in transaction`() {
    assertChecksThreadConfinement<TransactionWithoutReturn>(
      transacter = transacter,
      scope = { transaction(false, it) },
      block = { rollback() },
    )
  }

  @Test fun `detect the afterRollback call has escaped the original transaction thread in transactionWithReturn`() {
    assertChecksThreadConfinement<TransactionWithReturn<Unit>>(
      transacter = transacter,
      scope = { transactionWithResult(false, it) },
      block = { afterRollback {} },
    )
  }

  @Test fun `detect the afterCommit call has escaped the original transaction thread in transactionWithReturn`() {
    assertChecksThreadConfinement<TransactionWithReturn<Unit>>(
      transacter = transacter,
      scope = { transactionWithResult(false, it) },
      block = { afterCommit {} },
    )
  }

  @Test fun `detect the rollback call has escaped the original transaction thread in transactionWithReturn`() {
    assertChecksThreadConfinement<TransactionWithReturn<Unit>>(
      transacter = transacter,
      scope = { transactionWithResult<Unit>(false, it) },
      block = { rollback(Unit) },
    )
  }
}
