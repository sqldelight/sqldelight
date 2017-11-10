package com.squareup.sqldelight.core

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DatabaseTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `database has all unlabeled statements`() {
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

    val databaseFile = result.compilerOutput[File(result.fixtureRootDir, "com/example/Database.kt")]
    assertThat(databaseFile).isNotNull()
    assertThat(databaseFile.toString()).isEqualTo("""
      |package com.example
      |
      |import android.arch.persistence.db.SupportSQLiteDatabase
      |import android.arch.persistence.db.SupportSQLiteOpenHelper
      |import com.squareup.sqldelight.SqlDelightDatabase
      |import kotlin.Int
      |
      |class Database(openHelper: SupportSQLiteOpenHelper) : SqlDelightDatabase(openHelper) {
      |  companion object {
      |    fun callback(version: Int): SupportSQLiteOpenHelper.Callback = object : SupportSQLiteOpenHelper.Callback(version) {
      |      override fun onCreate(db: SupportSQLiteDatabase) {
      |        db.execSql(""${'"'}
      |            |CREATE TABLE test_table(
      |            |  _id INTEGER NOT NULL PRIMARY KEY,
      |            |  value TEXT
      |            |)
      |            ""${'"'}.trimMargin())
      |        db.execSql(""${'"'}
      |            |INSERT INTO test_table
      |            |VALUES (1, 'test')
      |            ""${'"'}.trimMargin())
      |      }
      |    }
      |  }
      |}
      |
      """.trimMargin())
  }

  @Test fun `database has adapter properties`() {
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

    val databaseFile = result.compilerOutput[File(result.fixtureRootDir, "com/example/Database.kt")]
    assertThat(databaseFile).isNotNull()
    assertThat(databaseFile.toString()).isEqualTo(
        """
        |package com.example
        |
        |import android.arch.persistence.db.SupportSQLiteDatabase
        |import android.arch.persistence.db.SupportSQLiteOpenHelper
        |import com.squareup.sqldelight.SqlDelightDatabase
        |import kotlin.Int
        |
        |class Database(
        |    openHelper: SupportSQLiteOpenHelper,
        |    internal val test_tableAdapter: Test_table.Adapter,
        |    internal val test_table2Adapter: Test_table2.Adapter
        |) : SqlDelightDatabase(openHelper) {
        |  companion object {
        |    fun callback(version: Int): SupportSQLiteOpenHelper.Callback = object : SupportSQLiteOpenHelper.Callback(version) {
        |      override fun onCreate(db: SupportSQLiteDatabase) {
        |        db.execSql(""${'"'}
        |            |CREATE TABLE test_table(
        |            |  _id INTEGER NOT NULL PRIMARY KEY,
        |            |  value TEXT AS List<String>
        |            |)
        |            ""${'"'}.trimMargin())
        |        db.execSql(""${'"'}
        |            |CREATE TABLE test_table2(
        |            |  _id INTEGER NOT NULL PRIMARY KEY,
        |            |  value TEXT AS List<String>
        |            |)
        |            ""${'"'}.trimMargin())
        |      }
        |    }
        |  }
        |}
        |
        """.trimMargin())
  }
}