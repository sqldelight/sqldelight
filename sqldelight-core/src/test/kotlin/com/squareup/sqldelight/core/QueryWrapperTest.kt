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
      |import com.squareup.sqldelight.Transacter
      |import com.squareup.sqldelight.db.SqlDatabase
      |import com.squareup.sqldelight.db.SqlDatabaseConnection
      |import com.squareup.sqldelight.db.SqlPreparedStatement
      |import java.lang.ThreadLocal
      |import kotlin.Int
      |
      |class QueryWrapper(database: SqlDatabase) {
      |    private val transactions: ThreadLocal<Transacter.Transaction> =
      |            ThreadLocal<Transacter.Transaction>()
      |
      |    val testQueries: TestQueries = TestQueries(this, database, transactions)
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
        |import com.squareup.sqldelight.Transacter
        |import com.squareup.sqldelight.db.SqlDatabase
        |import com.squareup.sqldelight.db.SqlDatabaseConnection
        |import com.squareup.sqldelight.db.SqlPreparedStatement
        |import java.lang.ThreadLocal
        |import kotlin.Int
        |
        |class QueryWrapper(
        |        database: SqlDatabase,
        |        internal val test_tableAdapter: Test_table.Adapter,
        |        internal val test_table2Adapter: Test_table2.Adapter
        |) {
        |    private val transactions: ThreadLocal<Transacter.Transaction> =
        |            ThreadLocal<Transacter.Transaction>()
        |
        |    val testQueries: TestQueries = TestQueries(this, database, transactions)
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
}