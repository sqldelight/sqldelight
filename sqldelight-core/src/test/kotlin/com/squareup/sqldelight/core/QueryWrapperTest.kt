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

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/QueryWrapper.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo("""
      |package com.example
      |
      |import com.squareup.sqldelight.db.SqlDatabase
      |import com.squareup.sqldelight.db.SqlDatabaseConnection
      |import com.squareup.sqldelight.db.SqlPreparedStatement
      |import kotlin.Int
      |
      |class QueryWrapper(database: SqlDatabase) {
      |    val testQueries: TestQueries = TestQueries(this, database)
      |    companion object : SqlDatabase.Helper {
      |        override fun onCreate(db: SqlDatabaseConnection) {
      |            db.prepareStatement(""${'"'}
      |                    |CREATE TABLE test_table(
      |                    |  _id INTEGER NOT NULL PRIMARY KEY,
      |                    |  value TEXT
      |                    |)
      |                    ""${'"'}.trimMargin(), SqlPreparedStatement.Type.EXEC).execute()
      |            db.prepareStatement(""${'"'}
      |                    |INSERT INTO test_table
      |                    |VALUES (1, 'test')
      |                    ""${'"'}.trimMargin(), SqlPreparedStatement.Type.EXEC).execute()
      |        }
      |
      |        override fun onMigrate(
      |                db: SqlDatabaseConnection,
      |                oldVersion: Int,
      |                newVersion: Int
      |        ) {
      |        }
      |    }
      |}
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

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/QueryWrapper.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo("""
        |package com.example
        |
        |import com.squareup.sqldelight.db.SqlDatabase
        |import com.squareup.sqldelight.db.SqlDatabaseConnection
        |import com.squareup.sqldelight.db.SqlPreparedStatement
        |import kotlin.Int
        |
        |class QueryWrapper(
        |        database: SqlDatabase,
        |        internal val test_tableAdapter: Test_table.Adapter,
        |        internal val test_table2Adapter: Test_table2.Adapter
        |) {
        |    val testQueries: TestQueries = TestQueries(this, database)
        |    companion object : SqlDatabase.Helper {
        |        override fun onCreate(db: SqlDatabaseConnection) {
        |            db.prepareStatement(""${'"'}
        |                    |CREATE TABLE test_table(
        |                    |  _id INTEGER NOT NULL PRIMARY KEY,
        |                    |  value TEXT
        |                    |)
        |                    ""${'"'}.trimMargin(), SqlPreparedStatement.Type.EXEC).execute()
        |            db.prepareStatement(""${'"'}
        |                    |CREATE TABLE test_table2(
        |                    |  _id INTEGER NOT NULL PRIMARY KEY,
        |                    |  value TEXT
        |                    |)
        |                    ""${'"'}.trimMargin(), SqlPreparedStatement.Type.EXEC).execute()
        |        }
        |
        |        override fun onMigrate(
        |                db: SqlDatabaseConnection,
        |                oldVersion: Int,
        |                newVersion: Int
        |        ) {
        |        }
        |    }
        |}
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

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/QueryWrapper.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo("""
        |package com.example
        |
        |import com.squareup.sqldelight.db.SqlDatabase
        |import com.squareup.sqldelight.db.SqlDatabaseConnection
        |import com.squareup.sqldelight.db.SqlPreparedStatement
        |import kotlin.Int
        |
        |class QueryWrapper(database: SqlDatabase) {
        |    val testQueries: TestQueries = TestQueries(this, database)
        |    companion object : SqlDatabase.Helper {
        |        override fun onCreate(db: SqlDatabaseConnection) {
        |            db.prepareStatement(""${'"'}
        |                    |CREATE VIEW A AS
        |                    |SELECT 1
        |                    ""${'"'}.trimMargin(), SqlPreparedStatement.Type.EXEC).execute()
        |            db.prepareStatement(""${'"'}
        |                    |CREATE VIEW B AS
        |                    |SELECT *
        |                    |FROM A
        |                    ""${'"'}.trimMargin(), SqlPreparedStatement.Type.EXEC).execute()
        |        }
        |
        |        override fun onMigrate(
        |                db: SqlDatabaseConnection,
        |                oldVersion: Int,
        |                newVersion: Int
        |        ) {
        |        }
        |    }
        |}
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

    val queryWrapperFile = result.compilerOutput[File(result.outputDirectory, "com/example/QueryWrapper.kt")]
    assertThat(queryWrapperFile).isNotNull()
    assertThat(queryWrapperFile.toString()).isEqualTo("""
        |package com.example
        |
        |import com.squareup.sqldelight.db.SqlDatabase
        |import com.squareup.sqldelight.db.SqlDatabaseConnection
        |import com.squareup.sqldelight.db.SqlPreparedStatement
        |import kotlin.Int
        |
        |class QueryWrapper(database: SqlDatabase) {
        |    val testQueries: TestQueries = TestQueries(this, database)
        |    companion object : SqlDatabase.Helper {
        |        override fun onCreate(db: SqlDatabaseConnection) {
        |            db.prepareStatement(""${'"'}
        |                    |CREATE TABLE test (
        |                    |  value TEXT
        |                    |)
        |                    ""${'"'}.trimMargin(), SqlPreparedStatement.Type.EXEC).execute()
        |            db.prepareStatement(""${'"'}
        |                    |CREATE TRIGGER A
        |                    |BEFORE DELETE ON test
        |                    |BEGIN
        |                    |INSERT INTO test DEFAULT VALUES;
        |                    |END
        |                    ""${'"'}.trimMargin(), SqlPreparedStatement.Type.EXEC).execute()
        |            db.prepareStatement("CREATE INDEX B ON test(value)", SqlPreparedStatement.Type.EXEC).execute()
        |        }
        |
        |        override fun onMigrate(
        |                db: SqlDatabaseConnection,
        |                oldVersion: Int,
        |                newVersion: Int
        |        ) {
        |        }
        |    }
        |}
        |
        """.trimMargin())
  }
}