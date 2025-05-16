package app.cash.sqldelight.core

import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.fixtureRoot
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class QueryWrapperTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `queryWrapper create method has all unlabeled statements`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test_table(
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT
      |);
      |
      |INSERT INTO test_table
      |VALUES (1, 'test');
      """.trimMargin(),
      tempFolder,
    )

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo(
      """
      |package com.example.testmodule
      |
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.AfterVersion
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlDriver
      |import app.cash.sqldelight.db.SqlSchema
      |import com.example.TestDatabase
      |import kotlin.Long
      |import kotlin.Unit
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
      |  get() = TestDatabaseImpl.Schema
      |
      |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
      |    TestDatabaseImpl(driver)
      |
      |private class TestDatabaseImpl(
      |  driver: SqlDriver,
      |) : TransacterImpl(driver),
      |    TestDatabase {
      |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
      |    override val version: Long
      |      get() = 1
      |
      |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      |      driver.execute(null, ""${'"'}
      |          |CREATE TABLE test_table(
      |          |  _id INTEGER NOT NULL PRIMARY KEY,
      |          |  value TEXT
      |          |)
      |          ""${'"'}.trimMargin(), 0)
      |      driver.execute(null, ""${'"'}
      |          |INSERT INTO test_table
      |          |VALUES (1, 'test')
      |          ""${'"'}.trimMargin(), 0)
      |      return QueryResult.Unit
      |    }
      |
      |    override fun migrate(
      |      driver: SqlDriver,
      |      oldVersion: Long,
      |      newVersion: Long,
      |      vararg callbacks: AfterVersion,
      |    ): QueryResult.Value<Unit> = QueryResult.Unit
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `queryWrapper has adapter properties`() {
    val result = FixtureCompiler.compileSql(
      """
      |import java.util.List;
      |
      |CREATE TABLE test_table(
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS List<String>
      |);
      |CREATE TABLE test_table2(
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS List<String>
      |);
      |
      |query:
      |SELECT value FROM test_table
      |UNION ALL
      |SELECT value FROM test_table2;
      """.trimMargin(),
      tempFolder,
    )

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo(
      """
        |package com.example.testmodule
        |
        |import app.cash.sqldelight.TransacterImpl
        |import app.cash.sqldelight.db.AfterVersion
        |import app.cash.sqldelight.db.QueryResult
        |import app.cash.sqldelight.db.SqlDriver
        |import app.cash.sqldelight.db.SqlSchema
        |import com.example.TestDatabase
        |import com.example.TestQueries
        |import com.example.Test_table
        |import com.example.Test_table2
        |import kotlin.Long
        |import kotlin.Unit
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
        |  get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(
        |  driver: SqlDriver,
        |  test_table2Adapter: Test_table2.Adapter,
        |  test_tableAdapter: Test_table.Adapter,
        |): TestDatabase = TestDatabaseImpl(driver, test_table2Adapter, test_tableAdapter)
        |
        |private class TestDatabaseImpl(
        |  driver: SqlDriver,
        |  test_table2Adapter: Test_table2.Adapter,
        |  test_tableAdapter: Test_table.Adapter,
        |) : TransacterImpl(driver),
        |    TestDatabase {
        |  override val testQueries: TestQueries = TestQueries(driver, test_tableAdapter, test_table2Adapter)
        |
        |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
        |    override val version: Long
        |      get() = 1
        |
        |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE test_table(
        |          |  _id INTEGER NOT NULL PRIMARY KEY,
        |          |  value TEXT
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE test_table2(
        |          |  _id INTEGER NOT NULL PRIMARY KEY,
        |          |  value TEXT
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      return QueryResult.Unit
        |    }
        |
        |    override fun migrate(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |      vararg callbacks: AfterVersion,
        |    ): QueryResult.Value<Unit> = QueryResult.Unit
        |  }
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun `queryWrapper writes tables in correct order`() {
    val result = FixtureCompiler.compileSql(
      """
        CREATE TABLE child(
          parent_id INTEGER REFERENCES parent(id)
        );

        CREATE TABLE parent(
          id INTEGER PRIMARY KEY
        );
      """.trimIndent(),
      tempFolder,
      overrideDialect = PostgreSqlDialect(),
    )

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]

    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo(
      """
        |package com.example.testmodule
        |
        |import app.cash.sqldelight.TransacterImpl
        |import app.cash.sqldelight.db.AfterVersion
        |import app.cash.sqldelight.db.QueryResult
        |import app.cash.sqldelight.db.SqlDriver
        |import app.cash.sqldelight.db.SqlSchema
        |import com.example.TestDatabase
        |import kotlin.Long
        |import kotlin.Unit
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
        |  get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |    TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(
        |  driver: SqlDriver,
        |) : TransacterImpl(driver),
        |    TestDatabase {
        |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
        |    override val version: Long
        |      get() = 1
        |
        |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE parent(
        |          |  id INTEGER PRIMARY KEY
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE child(
        |          |  parent_id INTEGER REFERENCES parent(id)
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      return QueryResult.Unit
        |    }
        |
        |    override fun migrate(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |      vararg callbacks: AfterVersion,
        |    ): QueryResult.Value<Unit> = QueryResult.Unit
        |  }
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun `queryWrapper errors on tables with cyclic references without allowing cycles`() {
    assertFailsWith<IllegalStateException> {
      FixtureCompiler.compileSql(
        """
        |CREATE TABLE child(
        |  id INTEGER PRIMARY KEY,
        |  parent_id INTEGER REFERENCES parent(id)
        |);
        |
        |CREATE TABLE parent(
        |  id INTEGER PRIMARY KEY,
        |  child_id INTEGER REFERENCES child(id)
        |);
        """.trimMargin(),
        overrideDialect = PostgreSqlDialect(),
        temporaryFolder = tempFolder,
      )
    }
  }

  @Test fun `queryWrapper writes cyclic tables when allowing reference cycles`() {
    val result = FixtureCompiler.compileSql(
      """
        |CREATE TABLE parent(
        |  id INTEGER PRIMARY KEY,
        |  child_id INTEGER REFERENCES child(id)
        |);
        |
        |CREATE TABLE child(
        |  id INTEGER PRIMARY KEY,
        |  parent_id INTEGER REFERENCES parent(id)
        |);
      """.trimMargin(),
      temporaryFolder = tempFolder,
    )

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo(
      """
        |package com.example.testmodule
        |
        |import app.cash.sqldelight.TransacterImpl
        |import app.cash.sqldelight.db.AfterVersion
        |import app.cash.sqldelight.db.QueryResult
        |import app.cash.sqldelight.db.SqlDriver
        |import app.cash.sqldelight.db.SqlSchema
        |import com.example.TestDatabase
        |import kotlin.Long
        |import kotlin.Unit
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
        |  get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |    TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(
        |  driver: SqlDriver,
        |) : TransacterImpl(driver),
        |    TestDatabase {
        |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
        |    override val version: Long
        |      get() = 1
        |
        |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE parent(
        |          |  id INTEGER PRIMARY KEY,
        |          |  child_id INTEGER REFERENCES child(id)
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE child(
        |          |  id INTEGER PRIMARY KEY,
        |          |  parent_id INTEGER REFERENCES parent(id)
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      return QueryResult.Unit
        |    }
        |
        |    override fun migrate(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |      vararg callbacks: AfterVersion,
        |    ): QueryResult.Value<Unit> = QueryResult.Unit
        |  }
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun `complicated table ordering works`() {
    val result = FixtureCompiler.compileSql(
      """
        |CREATE TABLE child1(
        |  id INTEGER PRIMARY KEY,
        |  parent_id INTEGER REFERENCES parent(id)
        |);
        |
        |CREATE TABLE grandchild(
        |  parent1_id INTEGER REFERENCES child1(id),
        |  parent2_id INTEGER REFERENCES child2(id)
        |);
        |
        |CREATE TABLE parent(
        |  id INTEGER PRIMARY KEY
        |);
        |
        |CREATE TABLE child2(
        |  id INTEGER PRIMARY KEY,
        |  parent_id INTEGER REFERENCES parent(id)
        |);
      """.trimMargin(),
      overrideDialect = PostgreSqlDialect(),
      temporaryFolder = tempFolder,
    )

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo(
      """
        |package com.example.testmodule
        |
        |import app.cash.sqldelight.TransacterImpl
        |import app.cash.sqldelight.db.AfterVersion
        |import app.cash.sqldelight.db.QueryResult
        |import app.cash.sqldelight.db.SqlDriver
        |import app.cash.sqldelight.db.SqlSchema
        |import com.example.TestDatabase
        |import kotlin.Long
        |import kotlin.Unit
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
        |  get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |    TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(
        |  driver: SqlDriver,
        |) : TransacterImpl(driver),
        |    TestDatabase {
        |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
        |    override val version: Long
        |      get() = 1
        |
        |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE parent(
        |          |  id INTEGER PRIMARY KEY
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE child1(
        |          |  id INTEGER PRIMARY KEY,
        |          |  parent_id INTEGER REFERENCES parent(id)
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE child2(
        |          |  id INTEGER PRIMARY KEY,
        |          |  parent_id INTEGER REFERENCES parent(id)
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE grandchild(
        |          |  parent1_id INTEGER REFERENCES child1(id),
        |          |  parent2_id INTEGER REFERENCES child2(id)
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      return QueryResult.Unit
        |    }
        |
        |    override fun migrate(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |      vararg callbacks: AfterVersion,
        |    ): QueryResult.Value<Unit> = QueryResult.Unit
        |  }
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun `queryWrapper puts views in correct order`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE VIEW B AS
      |SELECT *
      |FROM A;
      |
      |CREATE VIEW A AS
      |SELECT 1;
      """.trimMargin(),
      tempFolder,
    )

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo(
      """
        |package com.example.testmodule
        |
        |import app.cash.sqldelight.TransacterImpl
        |import app.cash.sqldelight.db.AfterVersion
        |import app.cash.sqldelight.db.QueryResult
        |import app.cash.sqldelight.db.SqlDriver
        |import app.cash.sqldelight.db.SqlSchema
        |import com.example.TestDatabase
        |import kotlin.Long
        |import kotlin.Unit
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
        |  get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |    TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(
        |  driver: SqlDriver,
        |) : TransacterImpl(driver),
        |    TestDatabase {
        |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
        |    override val version: Long
        |      get() = 1
        |
        |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        |      driver.execute(null, ""${'"'}
        |          |CREATE VIEW A AS
        |          |SELECT 1
        |          ""${'"'}.trimMargin(), 0)
        |      driver.execute(null, ""${'"'}
        |          |CREATE VIEW B AS
        |          |SELECT *
        |          |FROM A
        |          ""${'"'}.trimMargin(), 0)
        |      return QueryResult.Unit
        |    }
        |
        |    override fun migrate(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |      vararg callbacks: AfterVersion,
        |    ): QueryResult.Value<Unit> = QueryResult.Unit
        |  }
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun `queryWrapper puts triggers and ind in correct order`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TRIGGER A
      |BEFORE DELETE ON test
      |BEGIN
      |INSERT INTO test DEFAULT VALUES;
      |END;
      |
      |CREATE INDEX B ON test(value);
      |
      |CREATE TABLE test (
      |  value TEXT
      |);
      """.trimMargin(),
      tempFolder,
    )

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo(
      """
        |package com.example.testmodule
        |
        |import app.cash.sqldelight.TransacterImpl
        |import app.cash.sqldelight.db.AfterVersion
        |import app.cash.sqldelight.db.QueryResult
        |import app.cash.sqldelight.db.SqlDriver
        |import app.cash.sqldelight.db.SqlSchema
        |import com.example.TestDatabase
        |import kotlin.Long
        |import kotlin.Unit
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
        |  get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |    TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(
        |  driver: SqlDriver,
        |) : TransacterImpl(driver),
        |    TestDatabase {
        |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
        |    override val version: Long
        |      get() = 1
        |
        |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE test (
        |          |  value TEXT
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      driver.execute(null, ""${'"'}
        |          |CREATE TRIGGER A
        |          |BEFORE DELETE ON test
        |          |BEGIN
        |          |INSERT INTO test DEFAULT VALUES;
        |          |END
        |          ""${'"'}.trimMargin(), 0)
        |      driver.execute(null, "CREATE INDEX B ON test(value)", 0)
        |      return QueryResult.Unit
        |    }
        |
        |    override fun migrate(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |      vararg callbacks: AfterVersion,
        |    ): QueryResult.Value<Unit> = QueryResult.Unit
        |  }
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun `queryWrapper puts foreign key constraint in correct order`() {
    val result = FixtureCompiler.compileSql(
      """
        CREATE TABLE child(
          parent_id INTEGER,

          FOREIGN KEY (parent_id) REFERENCES parent(id)
        );

        CREATE TABLE parent(
          id INTEGER PRIMARY KEY
        );
      """.trimIndent(),
      tempFolder,
      overrideDialect = PostgreSqlDialect(),
    )

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]

    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo(
      """
        |package com.example.testmodule
        |
        |import app.cash.sqldelight.TransacterImpl
        |import app.cash.sqldelight.db.AfterVersion
        |import app.cash.sqldelight.db.QueryResult
        |import app.cash.sqldelight.db.SqlDriver
        |import app.cash.sqldelight.db.SqlSchema
        |import com.example.TestDatabase
        |import kotlin.Long
        |import kotlin.Unit
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
        |  get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |    TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(
        |  driver: SqlDriver,
        |) : TransacterImpl(driver),
        |    TestDatabase {
        |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
        |    override val version: Long
        |      get() = 1
        |
        |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE parent(
        |          |  id INTEGER PRIMARY KEY
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE child(
        |          |  parent_id INTEGER,
        |          |
        |          |  FOREIGN KEY (parent_id) REFERENCES parent(id)
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      return QueryResult.Unit
        |    }
        |
        |    override fun migrate(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |      vararg callbacks: AfterVersion,
        |    ): QueryResult.Value<Unit> = QueryResult.Unit
        |  }
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun `queryWrapper generates with migration statements`() {
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
    )

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo(
      """
        |package com.example.testmodule
        |
        |import app.cash.sqldelight.TransacterImpl
        |import app.cash.sqldelight.db.AfterVersion
        |import app.cash.sqldelight.db.QueryResult
        |import app.cash.sqldelight.db.SqlDriver
        |import app.cash.sqldelight.db.SqlSchema
        |import com.example.TestDatabase
        |import kotlin.Long
        |import kotlin.Unit
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
        |  get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |    TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(
        |  driver: SqlDriver,
        |) : TransacterImpl(driver),
        |    TestDatabase {
        |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
        |    override val version: Long
        |      get() = 3
        |
        |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE test (
        |          |  value1 TEXT,
        |          |  value2 TEXT,
        |          |  value3 REAL
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      return QueryResult.Unit
        |    }
        |
        |    private fun migrateInternal(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |    ): QueryResult.Value<Unit> {
        |      if (oldVersion <= 0 && newVersion > 0) {
        |        driver.execute(null, ""${'"'}
        |            |CREATE TABLE test (
        |            |  value1 TEXT
        |            |)
        |            ""${'"'}.trimMargin(), 0)
        |      }
        |      if (oldVersion <= 1 && newVersion > 1) {
        |        driver.execute(null, "ALTER TABLE test ADD COLUMN value2 TEXT", 0)
        |      }
        |      if (oldVersion <= 2 && newVersion > 2) {
        |        driver.execute(null, "ALTER TABLE test ADD COLUMN value3 REAL", 0)
        |      }
        |      return QueryResult.Unit
        |    }
        |
        |    override fun migrate(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |      vararg callbacks: AfterVersion,
        |    ): QueryResult.Value<Unit> {
        |      var lastVersion = oldVersion
        |
        |      callbacks.filter { it.afterVersion in oldVersion until newVersion }
        |      .sortedBy { it.afterVersion }
        |      .forEach { callback ->
        |        migrateInternal(driver, oldVersion = lastVersion, newVersion = callback.afterVersion + 1)
        |        callback.block(driver)
        |        lastVersion = callback.afterVersion + 1
        |      }
        |
        |      if (lastVersion < newVersion) {
        |        migrateInternal(driver, lastVersion, newVersion)
        |      }
        |      return QueryResult.Unit
        |    }
        |  }
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun `pragmas defined in migrations are included in create(SqlDriver) function`() {
    FixtureCompiler.writeSql(
      """
      |PRAGMA journal_mode=wal;
      |CREATE TABLE test (
      |  value1 TEXT
      |);
      """.trimMargin(),
      tempFolder,
      "0.sqm",
    )
    FixtureCompiler.writeSql(
      """
      |query:
      |SELECT * FROM test;
      """.trimMargin(),
      tempFolder,
      "Query.sq",
    )
    val result = FixtureCompiler.compileFixture(
      tempFolder.fixtureRoot().path,
      deriveSchemaFromMigrations = true,
    )

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo(
      """
        |package com.example.testmodule
        |
        |import app.cash.sqldelight.TransacterImpl
        |import app.cash.sqldelight.db.AfterVersion
        |import app.cash.sqldelight.db.QueryResult
        |import app.cash.sqldelight.db.SqlDriver
        |import app.cash.sqldelight.db.SqlSchema
        |import com.example.QueryQueries
        |import com.example.TestDatabase
        |import kotlin.Long
        |import kotlin.Unit
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
        |  get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |    TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(
        |  driver: SqlDriver,
        |) : TransacterImpl(driver),
        |    TestDatabase {
        |  override val queryQueries: QueryQueries = QueryQueries(driver)
        |
        |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
        |    override val version: Long
        |      get() = 1
        |
        |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        |      driver.execute(null, "PRAGMA journal_mode=wal", 0)
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE test (
        |          |  value1 TEXT
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      return QueryResult.Unit
        |    }
        |
        |    private fun migrateInternal(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |    ): QueryResult.Value<Unit> {
        |      if (oldVersion <= 0 && newVersion > 0) {
        |        driver.execute(null, "PRAGMA journal_mode=wal", 0)
        |        driver.execute(null, ""${'"'}
        |            |CREATE TABLE test (
        |            |  value1 TEXT
        |            |)
        |            ""${'"'}.trimMargin(), 0)
        |      }
        |      return QueryResult.Unit
        |    }
        |
        |    override fun migrate(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |      vararg callbacks: AfterVersion,
        |    ): QueryResult.Value<Unit> {
        |      var lastVersion = oldVersion
        |
        |      callbacks.filter { it.afterVersion in oldVersion until newVersion }
        |      .sortedBy { it.afterVersion }
        |      .forEach { callback ->
        |        migrateInternal(driver, oldVersion = lastVersion, newVersion = callback.afterVersion + 1)
        |        callback.block(driver)
        |        lastVersion = callback.afterVersion + 1
        |      }
        |
        |      if (lastVersion < newVersion) {
        |        migrateInternal(driver, lastVersion, newVersion)
        |      }
        |      return QueryResult.Unit
        |    }
        |  }
        |}
        |
      """.trimMargin(),
    )
  }

  @Test fun `string longer than 2^16 is chunked`() {
    val sqString = buildString {
      append(
        """
        |CREATE TABLE class_ability_test (
        |  id TEXT PRIMARY KEY NOT NULL,
        |  class_id TEXT NOT NULL,
        |  name TEXT NOT NULL,
        |  level_id INTEGER NOT NULL DEFAULT 1,
        |  special TEXT,
        |  url TEXT NOT NULL
        |);
        |
        |INSERT INTO class_ability_test(id, class_id, name, level_id, special, url)
        |VALUES
        """.trimMargin(),
      )
      repeat(500) {
        if (it > 0) append(',')
        append("\n  ('class_01_ability_$it', 'class_01', 'aaaaaaaaaaaaaaa', 1, NULL, 'https://stuff.example.com/this/is/a/bunch/of/path/data/class_01_ability_$it.png')")
      }
      append(';')
    }
    val result = FixtureCompiler.compileSql(sqString, tempFolder)

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile.toString()).apply {
      startsWith(
        """
        |package com.example.testmodule
        |
        |import app.cash.sqldelight.TransacterImpl
        |import app.cash.sqldelight.db.AfterVersion
        |import app.cash.sqldelight.db.QueryResult
        |import app.cash.sqldelight.db.SqlDriver
        |import app.cash.sqldelight.db.SqlSchema
        |import com.example.TestDatabase
        |import kotlin.Long
        |import kotlin.Unit
        |import kotlin.reflect.KClass
        |import kotlin.text.buildString
        |
        |internal val KClass<TestDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
        |  get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |    TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(
        |  driver: SqlDriver,
        |) : TransacterImpl(driver),
        |    TestDatabase {
        |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
        |    override val version: Long
        |      get() = 1
        |
        |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        |      driver.execute(null, ""${'"'}
        |          |CREATE TABLE class_ability_test (
        |          |  id TEXT PRIMARY KEY NOT NULL,
        |          |  class_id TEXT NOT NULL,
        |          |  name TEXT NOT NULL,
        |          |  level_id INTEGER NOT NULL DEFAULT 1,
        |          |  special TEXT,
        |          |  url TEXT NOT NULL
        |          |)
        |          ""${'"'}.trimMargin(), 0)
        |      driver.execute(null, buildString(75_360) {
        |          append(""${'"'}
        |          |INSERT INTO class_ability_test(id, class_id, name, level_id, special, url)
        |          |VALUES
        |          |  ('class_01_ability_0', 'class_01', 'aaaaaaaaaaaaaaa', 1, NULL, 'https://stuff.example.com/this/is/a/bunch/of/path/data/class_01_ability_0.png')
        """.trimMargin(),
      )
      contains(
        """
        |          ""${'"'}.trimMargin())
        |          append(""${'"'}
        """.trimMargin(),
      )
      endsWith(
        """
        |          |  ('class_01_ability_499', 'class_01', 'aaaaaaaaaaaaaaa', 1, NULL, 'https://stuff.example.com/this/is/a/bunch/of/path/data/class_01_ability_499.png')
        |          ""${'"'}.trimMargin())
        |          }, 0)
        |      return QueryResult.Unit
        |    }
        |
        |    override fun migrate(
        |      driver: SqlDriver,
        |      oldVersion: Long,
        |      newVersion: Long,
        |      vararg callbacks: AfterVersion,
        |    ): QueryResult.Value<Unit> = QueryResult.Unit
        |  }
        |}
        |
        """.trimMargin(),
      )
    }
  }
}
