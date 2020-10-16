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
      |public override fun insertData(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data
      |  |VALUES (?, ?)
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, id?.let { it.toLong() })
      |    bindString(2, value?.let { database.data_Adapter.valueAdapter.encode(it) })
      |  }
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
      |public override fun updateItem(
      |  packageName: kotlin.String,
      |  className: kotlin.String,
      |  deprecated: kotlin.Boolean,
      |  link: kotlin.String
      |): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |UPDATE item
      |  |SET deprecated = ?,
      |  |    link = ?
      |  |WHERE packageName = ?
      |  |  AND className = ?
      |  ""${'"'}.trimMargin(), 4) {
      |    bindLong(1, if (deprecated) 1L else 0L)
      |    bindString(2, link)
      |    bindString(3, packageName)
      |    bindString(4, className)
      |  }
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
      |public override fun insertData(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data
      |  |VALUES (?, ?)
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, id?.let { it.toLong() })
      |    bindString(2, value?.let { database.data_Adapter.valueAdapter.encode(it) })
      |  }
      |  notifyQueries(${mutator.id}, {database.dataQueries.selectForId})
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
      |public override fun insertData(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data
      |  |VALUES (?, ?)
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, id?.let { it.toLong() })
      |    bindString(2, value?.let { database.data_Adapter.valueAdapter.encode(it) })
      |  }
      |  notifyQueries(${mutator.id}, {database.otherDataQueries.selectForId})
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
      |public override fun insertData(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data
      |  |VALUES (?, ?)
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, id?.let { it.toLong() })
      |    bindString(2, value?.let { database.data_Adapter.valueAdapter.encode(it) })
      |  }
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
      |public override fun insertData(id: kotlin.Int?, value: kotlin.collections.List<kotlin.String>?): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data
      |  |VALUES (?, ?)
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, id?.let { it.toLong() })
      |    bindString(2, value?.let { database.data_Adapter.valueAdapter.encode(it) })
      |  }
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
      |public override fun deleteData(): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |DELETE FROM data
      |  |WHERE id = 1
      |  |AND value IN (
      |  |  SELECT data.value
      |  |  FROM data
      |  |  INNER JOIN data AS data2
      |  |  ON data.id = data2.id
      |  |)
      |  ""${'"'}.trimMargin(), 0)
      |  notifyQueries(${mutator.id}, {database.dataQueries.selectForId})
      |}
      |""".trimMargin())
  }

  @Test fun `non null boolean binds fine`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER AS Int PRIMARY KEY,
      |  value INTEGER AS Boolean NOT NULL
      |);
      |
      |insertData:
      |INSERT INTO data (value)
      |VALUES (?);
      """.trimMargin(), tempFolder, fileName = "Data.sq")

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |public override fun insertData(value: kotlin.Boolean): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data (value)
      |  |VALUES (?)
      |  ""${'"'}.trimMargin(), 1) {
      |    bindLong(1, if (value) 1L else 0L)
      |  }
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
      |public override fun insertData(value: kotlin.ByteArray): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data (value)
      |  |VALUES (?)
      |  ""${'"'}.trimMargin(), 1) {
      |    bindBytes(1, value)
      |  }
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
      |public override fun insertData(value: kotlin.Double): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}
      |  |INSERT INTO data (value)
      |  |VALUES (?)
      |  ""${'"'}.trimMargin(), 1) {
      |    bindDouble(1, value)
      |  }
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
      |WHERE content LIKE '%' || ? || '%' ESCAPE '\'
      |;
      |""".trimMargin(), tempFolder, fileName = "Data.sq")

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo("""
      |public override fun insertItem(
      |  packageName: kotlin.String,
      |  className: kotlin.String,
      |  deprecated: kotlin.Boolean,
      |  link: kotlin.String
      |): kotlin.Unit {
      |  driver.execute(${mutator.id}, ""${'"'}INSERT OR FAIL INTO item(packageName, className, deprecated, link) VALUES (?, ?, ?, ?)""${'"'}, 4) {
      |    bindString(1, packageName)
      |    bindString(2, className)
      |    bindLong(3, if (deprecated) 1L else 0L)
      |    bindString(4, link)
      |  }
      |  notifyQueries(${mutator.id}, {database.dataQueries.queryTerm})
      |}
      |""".trimMargin())
  }

  @Test fun `insert with triggers and fts5 virtual tables is fine`() {
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
    |CREATE VIRTUAL TABLE item_index USING fts5(content TEXT, prefix='2 3 4 5 6 7', content_rowid=id);
    |
    |insertItem:
    |INSERT OR FAIL INTO item_index(content) VALUES (?);
    |
    |queryTerm:
    |SELECT item.*
    |FROM item_index
    |JOIN item ON (docid = item.id)
    |WHERE content MATCH '"one ' || ? || '" * ';
    |
    |""".trimMargin(), tempFolder, fileName = "Data.sq")

      val mutator = file.namedMutators.first()
      val generator = MutatorQueryGenerator(mutator)

      assertThat(generator.function().toString()).isEqualTo("""
    |public override fun insertItem(content: kotlin.String?): kotlin.Unit {
    |  driver.execute(${mutator.id}, ""${'"'}INSERT OR FAIL INTO item_index(content) VALUES (?)""${'"'}, 1) {
    |    bindString(1, content)
    |  }
    |  notifyQueries(${mutator.id}, {database.dataQueries.queryTerm})
    |}
    |""".trimMargin())
  }
}
