package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.createDatabaseManager
import co.touchlab.sqliter.deleteDatabase
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.NativeDatabaseManager
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.driver.test.DriverTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class IosDriverTest : DriverTest() {
  override fun setupDatabase(schema: SqlDatabase.Schema): SqlDatabase {
    val configuration = DatabaseConfiguration("testdb", 1, { connection ->
      wrapConnection(connection){
        schema.create(it)
      }
    })
    deleteDatabase(configuration.name)
    return SQLiterHelper({createDatabaseManager(configuration)})
  }

  @BeforeTest fun setup2() {
    super.setup()
  }

  @AfterTest fun tearDown2() {
    super.tearDown()
  }

  @Test fun `insert can run multiple times2`() {
    super.`insert can run multiple times`()
  }

  @Test fun `query can run multiple times2`() {
    super.`query can run multiple times`()
  }

  @Test fun `SqlResultSet getters return null if the column values are NULL2`() {
    super.`SqlResultSet getters return null if the column values are NULL`()
  }
}