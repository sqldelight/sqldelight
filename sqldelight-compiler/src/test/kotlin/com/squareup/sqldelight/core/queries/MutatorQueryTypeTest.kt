package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.MutatorQueryGenerator
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MutatorQueryTypeTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `type is generated properly for no result set changes`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<String>
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder)

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData {
      |    private val statement: com.squareup.sqldelight.db.SqlPreparedStatement by lazy {
      |            database.getConnection().prepareStatement(""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?, ?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 2)
      |            }
      |
      |    fun execute(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?) {
      |        statement.bindLong(1, if (id == null) null else id.toLong())
      |        statement.bindString(2, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |        statement.execute()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `type is generated properly for result set changes in same file`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<String>
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder, fileName = "Data.sq")

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData {
      |    private val statement: com.squareup.sqldelight.db.SqlPreparedStatement by lazy {
      |            database.getConnection().prepareStatement(""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?, ?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 2)
      |            }
      |
      |    fun execute(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?) {
      |        statement.bindLong(1, if (id == null) null else id.toLong())
      |        statement.bindString(2, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |        statement.execute()
      |        notifyQueries(queryWrapper.dataQueries.selectForId)
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `type is generated properly for result set changes in different file`() {
    FixtureCompiler.writeSql("""
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(), tempFolder, fileName = "OtherData.sq")

    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<String>
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder, fileName = "Data.sq")

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData {
      |    private val statement: com.squareup.sqldelight.db.SqlPreparedStatement by lazy {
      |            database.getConnection().prepareStatement(""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?, ?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 2)
      |            }
      |
      |    fun execute(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?) {
      |        statement.bindLong(1, if (id == null) null else id.toLong())
      |        statement.bindString(2, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |        statement.execute()
      |        notifyQueries(queryWrapper.otherDataQueries.selectForId)
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `type does not include selects with unchanged result sets`() {
    FixtureCompiler.writeSql("""
      |CREATE TABLE other_data (
      |  id INTEGER NOT NULL PRIMARY KEY
      |);
      |
      |selectForId:
      |SELECT *
      |FROM other_data
      |WHERE id = ?;
      """.trimMargin(), tempFolder, fileName = "OtherData.sq")

    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER AS Int NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<String>
      |);
      |
      |selectForId:
      |SELECT *
      |FROM other_data
      |WHERE id = ?;
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder, fileName = "Data.sq")

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData {
      |    private val statement: com.squareup.sqldelight.db.SqlPreparedStatement by lazy {
      |            database.getConnection().prepareStatement(""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?, ?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 2)
      |            }
      |
      |    fun execute(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?) {
      |        statement.bindLong(1, if (id == null) null else id.toLong())
      |        statement.bindString(2, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |        statement.execute()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `null can be passed for integer primary keys`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<String>
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder, fileName = "Data.sq")

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData {
      |    private val statement: com.squareup.sqldelight.db.SqlPreparedStatement by lazy {
      |            database.getConnection().prepareStatement(""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?, ?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 2)
      |            }
      |
      |    fun execute(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?) {
      |        statement.bindLong(1, if (id == null) null else id.toLong())
      |        statement.bindString(2, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |        statement.execute()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `mutator query has inner select`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<String>
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = 1;
      |
      |deleteData:
      |DELETE FROM data
      |WHERE id = 1
      |AND value IN (
      |  SELECT data.value
      |  FROM data
      |  INNER JOIN data AS data2
      |  ON data.id = data2.id
      |);
      """.trimMargin(), tempFolder, fileName = "Data.sq")

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class DeleteData {
      |    private val statement: com.squareup.sqldelight.db.SqlPreparedStatement by lazy {
      |            database.getConnection().prepareStatement(""${'"'}
      |            |DELETE FROM data
      |            |WHERE id = 1
      |            |AND value IN (
      |            |  SELECT data.value
      |            |  FROM data
      |            |  INNER JOIN data AS data2
      |            |  ON data.id = data2.id
      |            |)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.DELETE, 0)
      |            }
      |
      |    fun execute() {
      |        statement.execute()
      |        notifyQueries(queryWrapper.dataQueries.selectForId)
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `non null boolean binds fine`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value TEXT AS Boolean NOT NULL
      |);
      |
      |insertData:
      |INSERT INTO data (value)
      |VALUES (?);
      """.trimMargin(), tempFolder, fileName = "Data.sq")

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData {
      |    private val statement: com.squareup.sqldelight.db.SqlPreparedStatement by lazy {
      |            database.getConnection().prepareStatement(""${'"'}
      |            |INSERT INTO data (value)
      |            |VALUES (?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 1)
      |            }
      |
      |    fun execute(value: kotlin.Boolean) {
      |        statement.bindString(1, if (value) 1L else 0L)
      |        statement.execute()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `blob binds fine`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value BLOB NOT NULL
      |);
      |
      |insertData:
      |INSERT INTO data (value)
      |VALUES (?);
      """.trimMargin(), tempFolder, fileName = "Data.sq")

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData {
      |    private val statement: com.squareup.sqldelight.db.SqlPreparedStatement by lazy {
      |            database.getConnection().prepareStatement(""${'"'}
      |            |INSERT INTO data (value)
      |            |VALUES (?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 1)
      |            }
      |
      |    fun execute(value: kotlin.ByteArray) {
      |        statement.bindBytes(1, value)
      |        statement.execute()
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `real binds fine`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value REAL NOT NULL
      |);
      |
      |insertData:
      |INSERT INTO data (value)
      |VALUES (?);
      """.trimMargin(), tempFolder, fileName = "Data.sq")

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertData {
      |    private val statement: com.squareup.sqldelight.db.SqlPreparedStatement by lazy {
      |            database.getConnection().prepareStatement(""${'"'}
      |            |INSERT INTO data (value)
      |            |VALUES (?)
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 1)
      |            }
      |
      |    fun execute(value: kotlin.Double) {
      |        statement.bindDouble(1, value)
      |        statement.execute()
      |    }
      |}
      |""".trimMargin())
  }


  @Test fun `insert with triggers and virtual tables is fine`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE item(
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  packageName TEXT NOT NULL,
      |  className TEXT NOT NULL,
      |  deprecated INTEGER AS Boolean NOT NULL DEFAULT 0,
      |  link TEXT NOT NULL,
      |
      |  UNIQUE (packageName, className)
      |);
      |
      |CREATE VIRTUAL TABLE item_index USING fts4(content TEXT);
      |
      |insertItem:
      |INSERT OR FAIL INTO item(packageName, className, deprecated, link) VALUES (?, ?, ?, ?)
      |;
      |
      |queryTerm:
      |SELECT item.*
      |FROM item_index
      |JOIN item ON (docid = item.id)
      |WHERE content LIKE '%' || ?1 || '%' ESCAPE '\'
      |;
      |""".trimMargin(), tempFolder, fileName = "Data.sq")

    val generator = MutatorQueryGenerator(file.namedMutators.first())

    assertThat(generator.type().toString()).isEqualTo("""
      |private inner class InsertItem {
      |    private val statement: com.squareup.sqldelight.db.SqlPreparedStatement by lazy {
      |            database.getConnection().prepareStatement("INSERT OR FAIL INTO item(packageName, className, deprecated, link) VALUES (?, ?, ?, ?)", com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT, 4)
      |            }
      |
      |    fun execute(
      |        packageName: kotlin.String,
      |        className: kotlin.String,
      |        deprecated: kotlin.Boolean,
      |        link: kotlin.String
      |    ) {
      |        statement.bindString(1, packageName)
      |        statement.bindString(2, className)
      |        statement.bindLong(3, if (deprecated) 1L else 0L)
      |        statement.bindString(4, link)
      |        statement.execute()
      |        notifyQueries(queryWrapper.dataQueries.queryTerm)
      |    }
      |}
      |""".trimMargin())
  }
}