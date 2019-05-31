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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun insertData(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?) {
      |    driver.execute(Fixture0, ""${'"'}
      |    |INSERT INTO data
      |    |VALUES (?1, ?2)
      |    ""${'"'}.trimMargin(), 2) {
      |        bindLong(1, if (id == null) null else id.toLong())
      |        bindString(2, if (value == null) null else database.dataAdapter.valueAdapter.encode(value))
      |    }
      |}
      |""".trimMargin())
  }

  @Test fun `bind argument order is consistent with sql`() {
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
      |updateItem:
      |UPDATE item
      |SET deprecated = ?3,
      |    link = ?4
      |WHERE packageName = ?1
      |  AND className = ?2
      |;
      """.trimMargin(), tempFolder)

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun updateItem(
      |    packageName: kotlin.String,
      |    className: kotlin.String,
      |    deprecated: kotlin.Boolean,
      |    link: kotlin.String
      |) {
      |    driver.execute(${mutator.getQueryId()}, ""${'"'}
      |    |UPDATE item
      |    |SET deprecated = ?3,
      |    |    link = ?4
      |    |WHERE packageName = ?1
      |    |  AND className = ?2
      |    ""${'"'}.trimMargin(), 4) {
      |        bindLong(3, if (deprecated) 1L else 0L)
      |        bindString(4, link)
      |        bindString(1, packageName)
      |        bindString(2, className)
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun insertData(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?) {
      |    driver.execute(${mutator.getQueryId()}, ""${'"'}
      |    |INSERT INTO data
      |    |VALUES (?1, ?2)
      |    ""${'"'}.trimMargin(), 2) {
      |        bindLong(1, if (id == null) null else id.toLong())
      |        bindString(2, if (value == null) null else database.dataAdapter.valueAdapter.encode(value))
      |    }
      |    notifyQueries(database.dataQueries.selectForId)
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun insertData(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?) {
      |    driver.execute(${mutator.getQueryId()}, ""${'"'}
      |    |INSERT INTO data
      |    |VALUES (?1, ?2)
      |    ""${'"'}.trimMargin(), 2) {
      |        bindLong(1, if (id == null) null else id.toLong())
      |        bindString(2, if (value == null) null else database.dataAdapter.valueAdapter.encode(value))
      |    }
      |    notifyQueries(database.otherDataQueries.selectForId)
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun insertData(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?) {
      |    driver.execute(${mutator.getQueryId()}, ""${'"'}
      |    |INSERT INTO data
      |    |VALUES (?1, ?2)
      |    ""${'"'}.trimMargin(), 2) {
      |        bindLong(1, if (id == null) null else id.toLong())
      |        bindString(2, if (value == null) null else database.dataAdapter.valueAdapter.encode(value))
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun insertData(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?) {
      |    driver.execute(${mutator.getQueryId()}, ""${'"'}
      |    |INSERT INTO data
      |    |VALUES (?1, ?2)
      |    ""${'"'}.trimMargin(), 2) {
      |        bindLong(1, if (id == null) null else id.toLong())
      |        bindString(2, if (value == null) null else database.dataAdapter.valueAdapter.encode(value))
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun deleteData() {
      |    driver.execute(${mutator.getQueryId()}, ""${'"'}
      |    |DELETE FROM data
      |    |WHERE id = 1
      |    |AND value IN (
      |    |  SELECT data.value
      |    |  FROM data
      |    |  INNER JOIN data AS data2
      |    |  ON data.id = data2.id
      |    |)
      |    ""${'"'}.trimMargin(), 0)
      |    notifyQueries(database.dataQueries.selectForId)
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun insertData(value: kotlin.Boolean) {
      |    driver.execute(${mutator.getQueryId()}, ""${'"'}
      |    |INSERT INTO data (value)
      |    |VALUES (?1)
      |    ""${'"'}.trimMargin(), 1) {
      |        bindString(1, if (value) 1L else 0L)
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun insertData(value: kotlin.ByteArray) {
      |    driver.execute(${mutator.getQueryId()}, ""${'"'}
      |    |INSERT INTO data (value)
      |    |VALUES (?1)
      |    ""${'"'}.trimMargin(), 1) {
      |        bindBytes(1, value)
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun insertData(value: kotlin.Double) {
      |    driver.execute(${mutator.getQueryId()}, ""${'"'}
      |    |INSERT INTO data (value)
      |    |VALUES (?1)
      |    ""${'"'}.trimMargin(), 1) {
      |        bindDouble(1, value)
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

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |override fun insertItem(
      |    packageName: kotlin.String,
      |    className: kotlin.String,
      |    deprecated: kotlin.Boolean,
      |    link: kotlin.String
      |) {
      |    driver.execute(${mutator.getQueryId()}, ""${'"'}INSERT OR FAIL INTO item(packageName, className, deprecated, link) VALUES (?1, ?2, ?3, ?4)""${'"'}, 4) {
      |        bindString(1, packageName)
      |        bindString(2, className)
      |        bindLong(3, if (deprecated) 1L else 0L)
      |        bindString(4, link)
      |    }
      |    notifyQueries(database.dataQueries.queryTerm)
      |}
      |""".trimMargin())
  }
}