package com.squareup.sqldelight.kmp

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.kmp.KmpSqliteDriver
import com.squareup.sqldelight.driver.test.QueryTest
import org.junit.After
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder

class KmpQueryTest : QueryTest() {
  companion object {
    @JvmField
    @ClassRule
    val globalFolder: TemporaryFolder = TemporaryFolder().apply {
      create()
    }

    val database = globalFolder.newFile("test.db")
  }

  @After
  fun cleanupDatabase() {
    KmpDriverTest.database.delete()
  }

  override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
    return KmpSqliteDriver(BundledSQLiteDriver(), database.absolutePath, schema)
  }
}
