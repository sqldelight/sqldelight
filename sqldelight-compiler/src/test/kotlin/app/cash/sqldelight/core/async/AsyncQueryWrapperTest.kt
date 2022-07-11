package app.cash.sqldelight.core.async

import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

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
        |import app.cash.sqldelight.db.QueryResult
        |import app.cash.sqldelight.db.SqlDriver
        |import app.cash.sqldelight.db.SqlSchema
        |import com.example.TestDatabase
        |import kotlin.Int
        |import kotlin.Unit
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlSchema
        |  get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |    TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(
        |  driver: SqlDriver,
        |) : SuspendingTransacterImpl(driver), TestDatabase {
        |  public object Schema : SqlSchema {
        |    public override val version: Int
        |      get() = 3
        |
        |    public override fun create(driver: SqlDriver): QueryResult<Unit> = QueryResult.AsyncValue {
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE test (
        |          |  value1 TEXT,
        |          |  value2 TEXT,
        |          |  value3 REAL
        |          |)
        |          ""${'"'}.trimMargin(), 0).await()
        |    }
        |
        |    public override fun migrate(
        |      driver: SqlDriver,
        |      oldVersion: Int,
        |      newVersion: Int,
        |    ): QueryResult<Unit> = QueryResult.AsyncValue {
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
        |  }
        |}
        |
      """.trimMargin(),
    )
  }
}
