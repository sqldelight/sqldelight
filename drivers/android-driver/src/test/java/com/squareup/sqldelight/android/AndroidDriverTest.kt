package com.squareup.sqldelight.android

import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabase.Schema
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.driver.test.DriverTest
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidDriverTest : DriverTest() {
  override fun setupDatabase(schema: Schema): SqlDatabase {
    return AndroidSqlDatabase(schema, RuntimeEnvironment.application)
  }

  @Test
  fun `cached statement can be reused`() {
    val database = AndroidSqlDatabase(schema, RuntimeEnvironment.application, cacheSize = 1)
    val statement = database.prepareStatement(1, "SELECT * FROM test", SqlPreparedStatement.Type.SELECT, 0)

    val statement2 = database.prepareStatement(1, "SELECT * FROM test", SqlPreparedStatement.Type.SELECT, 0)

    assertSame(statement, statement2)
  }

  @Test
  fun `cached statement is evicted and closed`() {
    val database = AndroidSqlDatabase(schema, RuntimeEnvironment.application, cacheSize = 1)
    val statement = database.prepareStatement(1, "SELECT * FROM test", SqlPreparedStatement.Type.SELECT, 0)

    database.prepareStatement(2, "SELECT * FROM test", SqlPreparedStatement.Type.SELECT, 0)

    val statement3 = database.prepareStatement(1, "SELECT * FROM test", SqlPreparedStatement.Type.SELECT, 0)

    assertNotSame(statement, statement3)
  }
}
