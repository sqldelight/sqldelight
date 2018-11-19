package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.sqliter.deleteDatabase
import co.touchlab.sqliter.DatabaseConfiguration
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.driver.test.TransacterTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

class IosTransacterTest: TransacterTest() {
  override fun setupDatabase(schema: SqlDatabase.Schema): SqlDatabase {
    val configuration = DatabaseConfiguration("testdb", 1, { connection ->
      wrapConnection(connection){
        schema.create(it)
      }
    })
    deleteDatabase(configuration.name)
    return SQLiterHelper(createDatabaseManager(configuration))
  }

  // TODO: https://github.com/JetBrains/kotlin-native/issues/2328

  @BeforeTest fun setup2() {
    super.setup()
  }

  @AfterTest fun tearDown2() {
    super.teardown()
  }

  @Test fun `afterCommit runs after transaction commits2`() {
    super.`afterCommit runs after transaction commits`()
  }

  @Test fun `afterCommit does not run after transaction rollbacks2`() {
    super.`afterCommit does not run after transaction rollbacks`()
  }

  @Test fun `afterCommit runs after enclosing transaction commits2`() {
    super.`afterCommit runs after enclosing transaction commits`()
  }

  @Test fun `afterCommit does not run in nested transaction when enclosing rolls back2`() {
    super.`afterCommit does not run in nested transaction when enclosing rolls back`()
  }

  @Test fun `afterCommit does not run in nested transaction when nested rolls back2`() {
    super.`afterCommit does not run in nested transaction when nested rolls back`()
  }

  @Test fun `afterRollback no-ops if the transaction never rolls back2`() {
    super.`afterRollback no-ops if the transaction never rolls back`()
  }

  @Test fun `afterRollback runs after a rollback occurs2`() {
    super.`afterRollback runs after a rollback occurs`()
  }

  @Test fun `afterRollback runs after an inner transaction rolls back2`() {
    super.`afterRollback runs after an inner transaction rolls back`()
  }

  @Test fun `afterRollback runs in an inner transaction when the outer transaction rolls back2`() {
    super.`afterRollback runs in an inner transaction when the outer transaction rolls back`()
  }

  @Test fun `transactions close themselves out properly2`() {
    super.`transactions close themselves out properly`()
  }

  @Test fun `setting no enclosing fails if there is a currently running transaction2`() {
    super.`setting no enclosing fails if there is a currently running transaction`()
  }

  @Ignore @Test
  fun `An exception thrown in postRollback function is combined with the exception in the main body2`() {
    super.`An exception thrown in postRollback function is combined with the exception in the main body`()
  }
}