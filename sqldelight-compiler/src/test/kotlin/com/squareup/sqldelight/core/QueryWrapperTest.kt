package com.squareup.sqldelight.core

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class QueryWrapperTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `queryWrapper create method has all unlabeled statements`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE TABLE test_table(
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT
      |);
      |
      |INSERT INTO test_table
      |VALUES (1, 'test');
      """.trimMargin(), tempFolder)

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo("""
      |package com.example.testmodule
      |
      |import com.example.TestDatabase
      |import com.example.TestQueries
      |import com.squareup.sqldelight.TransacterImpl
      |import com.squareup.sqldelight.db.SqlDriver
      |import kotlin.Int
      |import kotlin.reflect.KClass
      |
      |internal val KClass<TestDatabase>.schema: SqlDriver.Schema
      |    get() = TestDatabaseImpl.Schema
      |
      |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
      |        TestDatabaseImpl(driver)
      |
      |private class TestDatabaseImpl(driver: SqlDriver) : TransacterImpl(driver), TestDatabase {
      |    override val testQueries: TestQueriesImpl = TestQueriesImpl(this, driver)
      |
      |    object Schema : SqlDriver.Schema {
      |        override val version: Int
      |            get() = 1
      |
      |        override fun create(driver: SqlDriver) {
      |            driver.execute(null,
      |                    "CREATE TABLE test_table(_id INTEGER NOT NULL PRIMARY KEY, value TEXT)", 0)
      |            driver.execute(null, "INSERT INTO test_table VALUES (1, 'test')", 0)
      |        }
      |
      |        override fun migrate(
      |            driver: SqlDriver,
      |            oldVersion: Int,
      |            newVersion: Int
      |        ) {
      |        }
      |    }
      |}
      |
      |private class TestQueriesImpl(private val database: TestDatabaseImpl, private val driver: SqlDriver)
      |        : TransacterImpl(driver), TestQueries
      |
      """.trimMargin())
  }

  @Test fun `queryWrapper has adapter properties`() {
    val result = FixtureCompiler.compileSql("""
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
      """.trimMargin(), tempFolder)

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo("""
        |package com.example.testmodule
        |
        |import com.example.TestDatabase
        |import com.example.TestQueries
        |import com.example.Test_table
        |import com.example.Test_table2
        |import com.squareup.sqldelight.TransacterImpl
        |import com.squareup.sqldelight.db.SqlDriver
        |import kotlin.Int
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlDriver.Schema
        |    get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(
        |    driver: SqlDriver,
        |    test_tableAdapter: Test_table.Adapter,
        |    test_table2Adapter: Test_table2.Adapter
        |): TestDatabase = TestDatabaseImpl(driver, test_tableAdapter, test_table2Adapter)
        |
        |private class TestDatabaseImpl(
        |    driver: SqlDriver,
        |    internal val test_tableAdapter: Test_table.Adapter,
        |    internal val test_table2Adapter: Test_table2.Adapter
        |) : TransacterImpl(driver), TestDatabase {
        |    override val testQueries: TestQueriesImpl = TestQueriesImpl(this, driver)
        |
        |    object Schema : SqlDriver.Schema {
        |        override val version: Int
        |            get() = 1
        |
        |        override fun create(driver: SqlDriver) {
        |            driver.execute(null,
        |                    "CREATE TABLE test_table(_id INTEGER NOT NULL PRIMARY KEY, value TEXT)", 0)
        |            driver.execute(null,
        |                    "CREATE TABLE test_table2(_id INTEGER NOT NULL PRIMARY KEY, value TEXT)", 0)
        |        }
        |
        |        override fun migrate(
        |            driver: SqlDriver,
        |            oldVersion: Int,
        |            newVersion: Int
        |        ) {
        |        }
        |    }
        |}
        |
        |private class TestQueriesImpl(private val database: TestDatabaseImpl, private val driver: SqlDriver)
        |        : TransacterImpl(driver), TestQueries
        |
        """.trimMargin())
  }

  @Test fun `queryWrapper puts views in correct order`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE VIEW B AS
      |SELECT *
      |FROM A;
      |
      |CREATE VIEW A AS
      |SELECT 1;
      """.trimMargin(), tempFolder)

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo("""
        |package com.example.testmodule
        |
        |import com.example.TestDatabase
        |import com.example.TestQueries
        |import com.squareup.sqldelight.TransacterImpl
        |import com.squareup.sqldelight.db.SqlDriver
        |import kotlin.Int
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlDriver.Schema
        |    get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |        TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(driver: SqlDriver) : TransacterImpl(driver), TestDatabase {
        |    override val testQueries: TestQueriesImpl = TestQueriesImpl(this, driver)
        |
        |    object Schema : SqlDriver.Schema {
        |        override val version: Int
        |            get() = 1
        |
        |        override fun create(driver: SqlDriver) {
        |            driver.execute(null, "CREATE VIEW A AS SELECT 1", 0)
        |            driver.execute(null, "CREATE VIEW B AS SELECT * FROM A", 0)
        |        }
        |
        |        override fun migrate(
        |            driver: SqlDriver,
        |            oldVersion: Int,
        |            newVersion: Int
        |        ) {
        |        }
        |    }
        |}
        |
        |private class TestQueriesImpl(private val database: TestDatabaseImpl, private val driver: SqlDriver)
        |        : TransacterImpl(driver), TestQueries
        |
        """.trimMargin())
  }

  @Test fun `queryWrapper puts triggers and ind in correct order`() {
    val result = FixtureCompiler.compileSql("""
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
      """.trimMargin(), tempFolder)

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo("""
        |package com.example.testmodule
        |
        |import com.example.TestDatabase
        |import com.example.TestQueries
        |import com.squareup.sqldelight.TransacterImpl
        |import com.squareup.sqldelight.db.SqlDriver
        |import kotlin.Int
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlDriver.Schema
        |    get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |        TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(driver: SqlDriver) : TransacterImpl(driver), TestDatabase {
        |    override val testQueries: TestQueriesImpl = TestQueriesImpl(this, driver)
        |
        |    object Schema : SqlDriver.Schema {
        |        override val version: Int
        |            get() = 1
        |
        |        override fun create(driver: SqlDriver) {
        |            driver.execute(null, "CREATE TABLE test (value TEXT)", 0)
        |            driver.execute(null,
        |                    "CREATE TRIGGER A BEFORE DELETE ON test BEGIN INSERT INTO test DEFAULT VALUES; END",
        |                    0)
        |            driver.execute(null, "CREATE INDEX B ON test(value)", 0)
        |        }
        |
        |        override fun migrate(
        |            driver: SqlDriver,
        |            oldVersion: Int,
        |            newVersion: Int
        |        ) {
        |        }
        |    }
        |}
        |
        |private class TestQueriesImpl(private val database: TestDatabaseImpl, private val driver: SqlDriver)
        |        : TransacterImpl(driver), TestQueries
        |
        """.trimMargin())
  }

  @Test fun `queryWrapper generates with migration statements`() {
    FixtureCompiler.writeSql("""
      |ALTER TABLE test ADD COLUMN value2 TEXT;
    """.trimMargin(), tempFolder, "1.sqm")
    FixtureCompiler.writeSql("""
      |ALTER TABLE test ADD COLUMN value3 REAL;
    """.trimMargin(), tempFolder, "2.sqm")
    val result = FixtureCompiler.compileSql("""
      |CREATE TABLE test (
      |  value1 TEXT,
      |  value2 TEXT,
      |  value3 REAL
      |);
      """.trimMargin(), tempFolder)

    assertThat(result.errors).isEmpty()

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/testmodule/TestDatabaseImpl.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo("""
        |package com.example.testmodule
        |
        |import com.example.TestDatabase
        |import com.example.TestQueries
        |import com.squareup.sqldelight.TransacterImpl
        |import com.squareup.sqldelight.db.SqlDriver
        |import kotlin.Int
        |import kotlin.reflect.KClass
        |
        |internal val KClass<TestDatabase>.schema: SqlDriver.Schema
        |    get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |        TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(driver: SqlDriver) : TransacterImpl(driver), TestDatabase {
        |    override val testQueries: TestQueriesImpl = TestQueriesImpl(this, driver)
        |
        |    object Schema : SqlDriver.Schema {
        |        override val version: Int
        |            get() = 3
        |
        |        override fun create(driver: SqlDriver) {
        |            driver.execute(null, "CREATE TABLE test (value1 TEXT, value2 TEXT, value3 REAL)", 0)
        |        }
        |
        |        override fun migrate(
        |            driver: SqlDriver,
        |            oldVersion: Int,
        |            newVersion: Int
        |        ) {
        |            if (oldVersion <= 1 && newVersion > 1) {
        |                driver.execute(null, "ALTER TABLE test ADD COLUMN value2 TEXT;", 0)
        |            }
        |            if (oldVersion <= 2 && newVersion > 2) {
        |                driver.execute(null, "ALTER TABLE test ADD COLUMN value3 REAL;", 0)
        |            }
        |        }
        |    }
        |}
        |
        |private class TestQueriesImpl(private val database: TestDatabaseImpl, private val driver: SqlDriver)
        |        : TransacterImpl(driver), TestQueries
        |
        """.trimMargin())
  }

  @Test fun `string longer than 2^16 is chunked`() {
    val sqString = buildString {
      append("""
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
        """.trimMargin())
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
      startsWith("""
        |package com.example.testmodule
        |
        |import com.example.TestDatabase
        |import com.example.TestQueries
        |import com.squareup.sqldelight.TransacterImpl
        |import com.squareup.sqldelight.db.SqlDriver
        |import kotlin.Int
        |import kotlin.reflect.KClass
        |import kotlin.text.buildString
        |
        |internal val KClass<TestDatabase>.schema: SqlDriver.Schema
        |    get() = TestDatabaseImpl.Schema
        |
        |internal fun KClass<TestDatabase>.newInstance(driver: SqlDriver): TestDatabase =
        |        TestDatabaseImpl(driver)
        |
        |private class TestDatabaseImpl(driver: SqlDriver) : TransacterImpl(driver), TestDatabase {
        |    override val testQueries: TestQueriesImpl = TestQueriesImpl(this, driver)
        |
        |    object Schema : SqlDriver.Schema {
        |        override val version: Int
        |            get() = 1
        |
        |        override fun create(driver: SqlDriver) {
        |            driver.execute(null,
        |                    "CREATE TABLE class_ability_test (id TEXT PRIMARY KEY NOT NULL, class_id TEXT NOT NULL, name TEXT NOT NULL, level_id INTEGER NOT NULL DEFAULT 1, special TEXT, url TEXT NOT NULL)",
        |                    0)
        |            driver.execute(null, buildString(74360) {
        |                    append("INSERT INTO class_ability_test(id, class_id, name, level_id, special, url) VALUES ('class_01_ability_0', 'class_01', 'aaaaaaaaaaaaaaa', 1, NULL, 'https://stuff.example.com/this/is/a/bunch/of/path/data/class_01_ability_0.png')
        """.trimMargin())
      contains("""
        |                    append("
      """.trimMargin())
      endsWith("""
        |('class_01_ability_499', 'class_01', 'aaaaaaaaaaaaaaa', 1, NULL, 'https://stuff.example.com/this/is/a/bunch/of/path/data/class_01_ability_499.png')")
        |                    }, 0)
        |        }
        |
        |        override fun migrate(
        |            driver: SqlDriver,
        |            oldVersion: Int,
        |            newVersion: Int
        |        ) {
        |        }
        |    }
        |}
        |
        |private class TestQueriesImpl(private val database: TestDatabaseImpl, private val driver: SqlDriver)
        |        : TransacterImpl(driver), TestQueries
        |
        """.trimMargin())
    }
  }
}
