package app.cash.sqldelight.core.async

import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AsyncQueryWrapperTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test
  fun `queryWrapper generates with migration statements`() {
    FixtureCompiler.writeSql(
      """
      |CREATE TABLE test (
      |  value1 TEXT
      |);
      """.trimMargin(),
      tempFolder,
      "0.sqm",
    )
    FixtureCompiler.writeSql(
      """
      |ALTER TABLE test ADD COLUMN value2 TEXT;
      """.trimMargin(),
      tempFolder,
      "1.sqm",
    )
    FixtureCompiler.writeSql(
      """
      |ALTER TABLE test ADD COLUMN value3 REAL;
      """.trimMargin(),
      tempFolder,
      "2.sqm",
    )
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  value1 TEXT,
      |  value2 TEXT,
      |  value3 REAL
      |);
      """.trimMargin(),
      tempFolder,
      generateAsync = true,
    )

    Truth.assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    Truth.assertThat(queryWrapperFile).isNotNull()
    Truth.assertThat(queryWrapperFile.toString()).isEqualTo(
      """
        |package com.example.testmodule
        |
        |import app.cash.sqldelight.SuspendingTransacterImpl
        |import app.cash.sqldelight.db.AfterVersion
        |import app.cash.sqldelight.db.QueryResult
        |import app.cash.sqldelight.db.SqlDriver
        |import app.cash.sqldelight.db.SqlSchema
        |import com.example.TestDatabase
        |import kotlin.Long
        |import kotlin.Unit
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.AsyncValue<Unit>>
        |  get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |    TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(
        |  driver: SqlDriver,
        |) : SuspendingTransacterImpl(driver), TestDatabase {
        |  public object Schema : SqlSchema<QueryResult.AsyncValue<Unit>> {
        |    override val version: Long
        |      get() = 3
        |
        |    override fun create(driver: SqlDriver): QueryResult.AsyncValue<Unit> = QueryResult.AsyncValue {
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE test (
        |          |  value1 TEXT,
        |          |  value2 TEXT,
        |          |  value3 REAL
        |          |)
        |          ""${'"'}.trimMargin(), 0).await()
        |    }
        |
        |    private fun migrateInternal(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |    ): QueryResult.AsyncValue<Unit> = QueryResult.AsyncValue {
        |      if (oldVersion <= 0 && newVersion > 0) {
        |        driver.execute(null, ""${'"'}
        |            |CREATE TABLE test (
        |            |  value1 TEXT
        |            |)
        |            ""${'"'}.trimMargin(), 0).await()
        |      }
        |      if (oldVersion <= 1 && newVersion > 1) {
        |        driver.execute(null, "ALTER TABLE test ADD COLUMN value2 TEXT", 0).await()
        |      }
        |      if (oldVersion <= 2 && newVersion > 2) {
        |        driver.execute(null, "ALTER TABLE test ADD COLUMN value3 REAL", 0).await()
        |      }
        |    }
        |
        |    override fun migrate(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |      vararg callbacks: AfterVersion,
        |    ): QueryResult.AsyncValue<Unit> = QueryResult.AsyncValue {
        |      var lastVersion = oldVersion
        |
        |      callbacks.filter { it.afterVersion in oldVersion until newVersion }
        |      .sortedBy { it.afterVersion }
        |      .forEach { callback ->
        |        migrateInternal(driver, oldVersion = lastVersion, newVersion = callback.afterVersion +
        |          1).await()
        |        callback.block(driver)
        |        lastVersion = callback.afterVersion + 1
        |      }
        |
        |      if (lastVersion < newVersion) {
        |        migrateInternal(driver, lastVersion, newVersion).await()
        |      }
        |    }
        |  }
        |}
        |
      """.trimMargin(),
    )
  }
}
