package com.squareup.sqldelight.core.queries

import com.alecstrong.sql.psi.core.DialectPreset
import com.google.common.truth.Truth.assertThat
import com.squareup.burst.BurstJUnit4
import com.squareup.sqldelight.core.compiler.SelectQueryGenerator
import com.squareup.sqldelight.core.dialects.textType
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(BurstJUnit4::class)
class SelectQueryTypeTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `query type generates properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      |""".trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  @kotlin.jvm.JvmField
      |  val id: kotlin.Long,
      |  mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
      |  override fun execute(): com.squareup.sqldelight.db.SqlCursor = driver.executeQuery(${query.id}, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE id = ?
      |  ""${'"'}.trimMargin(), 1) {
      |    bindLong(1, id)
      |  }
      |
      |  override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin())
  }

  @Test fun `bind arguments are ordered in generated type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |select:
      |SELECT *
      |FROM data
      |WHERE id = ?2
      |AND value = ?1;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectQuery<out T : kotlin.Any>(
      |  @kotlin.jvm.JvmField
      |  val value: kotlin.String,
      |  @kotlin.jvm.JvmField
      |  val id: kotlin.Long,
      |  mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(select, mapper) {
      |  override fun execute(): com.squareup.sqldelight.db.SqlCursor = driver.executeQuery(${query.id}, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE id = ?
      |  |AND value = ?
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, id)
      |    bindString(2, value)
      |  }
      |
      |  override fun toString(): kotlin.String = "Test.sq:select"
      |}
      |""".trimMargin())
  }

  @Test fun `array bind argument`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id IN ?;
      |""".trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  @kotlin.jvm.JvmField
      |  val id: kotlin.collections.Collection<kotlin.Long>,
      |  mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
      |  override fun execute(): com.squareup.sqldelight.db.SqlCursor {
      |    val idIndexes = createArguments(count = id.size, offset = 1)
      |    return driver.executeQuery(null, ""${'"'}
      |    |SELECT *
      |    |FROM data
      |    |WHERE id IN ${"$"}idIndexes
      |    ""${'"'}.trimMargin(), id.size) {
      |      id.forEachIndexed { index, id_ ->
      |          bindLong(index + 1, id_)
      |          }
      |    }
      |  }
      |
      |  override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin())
  }

  @Test fun `duplicated array bind argument`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  message TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id IN ?1 AND message != ?2 AND id IN ?1;
      |""".trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  @kotlin.jvm.JvmField
      |  val id: kotlin.collections.Collection<kotlin.Long>,
      |  @kotlin.jvm.JvmField
      |  val message: kotlin.String,
      |  mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
      |  override fun execute(): com.squareup.sqldelight.db.SqlCursor {
      |    val idIndexes = createArguments(count = id.size, offset = 1)
      |    return driver.executeQuery(null, ""${'"'}
      |    |SELECT *
      |    |FROM data
      |    |WHERE id IN ${"$"}idIndexes AND message != ? AND id IN ${"$"}idIndexes
      |    ""${'"'}.trimMargin(), 1 + id.size + id.size) {
      |      id.forEachIndexed { index, id_ ->
      |          bindLong(index + 1, id_)
      |          }
      |      bindString(id.size + 1, message)
      |      id.forEachIndexed { index, id__ ->
      |          bindLong(index + id.size + 2, id__)
      |          }
      |    }
      |  }
      |
      |  override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin())
  }

  @Test fun `nullable parameter not escaped`() {
    val file = FixtureCompiler.parseSql("""
       |CREATE TABLE socialFeedItem (
       |  message TEXT,
       |  userId TEXT,
       |  creation_time INTEGER
       |);
       |
       |select_news_list:
       |SELECT * FROM socialFeedItem WHERE message IS NOT NULL AND userId = ? ORDER BY datetime(creation_time) DESC;
       |""".trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo("""
       |private inner class Select_news_listQuery<out T : kotlin.Any>(
       |  @kotlin.jvm.JvmField
       |  val userId: kotlin.String?,
       |  mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
       |) : com.squareup.sqldelight.Query<T>(select_news_list, mapper) {
       |  override fun execute(): com.squareup.sqldelight.db.SqlCursor = driver.executeQuery(null, ""${'"'}SELECT * FROM socialFeedItem WHERE message IS NOT NULL AND userId ${"$"}{ if (userId == null) "IS" else "=" } ? ORDER BY datetime(creation_time) DESC""${'"'}, 1) {
       |    bindString(1, userId)
       |  }
       |
       |  override fun toString(): kotlin.String = "Test.sq:select_news_list"
       |}
       |""".trimMargin())
  }

  @Test fun `nullable parameter has spaces`() {
    val file = FixtureCompiler.parseSql("""
       |CREATE TABLE IF NOT EXISTS Friend(
       |    _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
       |    username TEXT NOT NULL UNIQUE,
       |    userId TEXT
       |);
       |
       |selectData:
       |SELECT _id, username
       |FROM Friend
       |WHERE userId=? OR username=? LIMIT 2;
       |""".trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo("""
       |private inner class SelectDataQuery<out T : kotlin.Any>(
       |  @kotlin.jvm.JvmField
       |  val userId: kotlin.String?,
       |  @kotlin.jvm.JvmField
       |  val username: kotlin.String,
       |  mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
       |) : com.squareup.sqldelight.Query<T>(selectData, mapper) {
       |  override fun execute(): com.squareup.sqldelight.db.SqlCursor = driver.executeQuery(null, ""${'"'}
       |  |SELECT _id, username
       |  |FROM Friend
       |  |WHERE userId${'$'}{ if (userId == null) " IS " else "=" }? OR username=? LIMIT 2
       |  ""${'"'}.trimMargin(), 2) {
       |    bindString(1, userId)
       |    bindString(2, username)
       |  }
       |
       |  override fun toString(): kotlin.String = "Test.sq:selectData"
       |}
       |""".trimMargin())
  }

  @Test fun `nullable bind parameters`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  val TEXT
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE val = ?
      |AND val == ?
      |AND val <> ?
      |AND val != ?;
      |""".trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  @kotlin.jvm.JvmField
      |  val val_: kotlin.String?,
      |  @kotlin.jvm.JvmField
      |  val val__: kotlin.String?,
      |  @kotlin.jvm.JvmField
      |  val val___: kotlin.String?,
      |  @kotlin.jvm.JvmField
      |  val val____: kotlin.String?,
      |  mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
      |  override fun execute(): com.squareup.sqldelight.db.SqlCursor = driver.executeQuery(null, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE val ${"$"}{ if (val_ == null) "IS" else "=" } ?
      |  |AND val ${"$"}{ if (val__ == null) "IS" else "==" } ?
      |  |AND val ${"$"}{ if (val___ == null) "IS NOT" else "<>" } ?
      |  |AND val ${"$"}{ if (val____ == null) "IS NOT" else "!=" } ?
      |  ""${'"'}.trimMargin(), 4) {
      |    bindString(1, val_)
      |    bindString(2, val__)
      |    bindString(3, val___)
      |    bindString(4, val____)
      |  }
      |
      |  override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin())
  }

  @Test fun `synthesized column bind arguments`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE VIRTUAL TABLE data USING fts3 (
      |  content TEXT NOT NULL
      |);
      |
      |selectMatching:
      |SELECT *
      |FROM data
      |WHERE data MATCH ? AND rowid = ?;
      |""".trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectMatchingQuery<out T : kotlin.Any>(
      |  @kotlin.jvm.JvmField
      |  val data: kotlin.String,
      |  @kotlin.jvm.JvmField
      |  val rowid: kotlin.Long,
      |  mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(selectMatching, mapper) {
      |  override fun execute(): com.squareup.sqldelight.db.SqlCursor = driver.executeQuery(${query.id}, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE data MATCH ? AND rowid = ?
      |  ""${'"'}.trimMargin(), 2) {
      |    bindString(1, data)
      |    bindLong(2, rowid)
      |  }
      |
      |  override fun toString(): kotlin.String = "Test.sq:selectMatching"
      |}
      |""".trimMargin())
  }

  @Test fun `array and named bind arguments are compatible`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  token TEXT NOT NULL,
      |  name TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE token = :token
      |  AND id IN ?
      |  AND (token != :token OR (name = :name OR :name = 'foo'))
      |  AND token IN ?;
      |""".trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  @kotlin.jvm.JvmField
      |  val token: kotlin.String,
      |  @kotlin.jvm.JvmField
      |  val id: kotlin.collections.Collection<kotlin.Long>,
      |  @kotlin.jvm.JvmField
      |  val name: kotlin.String,
      |  @kotlin.jvm.JvmField
      |  val token_: kotlin.collections.Collection<kotlin.String>,
      |  mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
      |  override fun execute(): com.squareup.sqldelight.db.SqlCursor {
      |    val idIndexes = createArguments(count = id.size, offset = 2)
      |    val token_Indexes = createArguments(count = token_.size, offset = id.size + 5)
      |    return driver.executeQuery(null, ""${'"'}
      |    |SELECT *
      |    |FROM data
      |    |WHERE token = ?
      |    |  AND id IN ${"$"}idIndexes
      |    |  AND (token != ? OR (name = ? OR ? = 'foo'))
      |    |  AND token IN ${"$"}token_Indexes
      |    ""${'"'}.trimMargin(), 4 + id.size + token_.size) {
      |      bindString(1, token)
      |      id.forEachIndexed { index, id_ ->
      |          bindLong(index + 2, id_)
      |          }
      |      bindString(id.size + 2, token)
      |      bindString(id.size + 3, name)
      |      bindString(id.size + 4, name)
      |      token_.forEachIndexed { index, token__ ->
      |          bindString(index + id.size + 5, token__)
      |          }
      |    }
      |  }
      |
      |  override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin())
  }

  @Test fun `nullable parameter type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER
      |);
      |
      |selectForId:
      |WITH child_ids AS (SELECT id FROM data WHERE id = ?1)
      |SELECT *
      |FROM data
      |WHERE id = ?1 OR id IN child_ids
      |LIMIT :limit
      |OFFSET :offset;
      |""".trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  @kotlin.jvm.JvmField
      |  val id: kotlin.Long?,
      |  @kotlin.jvm.JvmField
      |  val limit: kotlin.Long,
      |  @kotlin.jvm.JvmField
      |  val offset: kotlin.Long,
      |  mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
      |  override fun execute(): com.squareup.sqldelight.db.SqlCursor = driver.executeQuery(null, ""${'"'}
      |  |WITH child_ids AS (SELECT id FROM data WHERE id ${'$'}{ if (id == null) "IS" else "=" } ?)
      |  |SELECT *
      |  |FROM data
      |  |WHERE id ${'$'}{ if (id == null) "IS" else "=" } ? OR id IN child_ids
      |  |LIMIT ?
      |  |OFFSET ?
      |  ""${'"'}.trimMargin(), 4) {
      |    bindLong(1, id)
      |    bindLong(2, id)
      |    bindLong(3, limit)
      |    bindLong(4, offset)
      |  }
      |
      |  override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin())
  }

  @Test fun `custom type vararg`() {
    val file = FixtureCompiler.parseSql("""
      |import foo.Bar;
      |
      |CREATE TABLE data (
      |  id INTEGER AS Bar
      |);
      |
      |selectForIds:
      |SELECT *
      |FROM data
      |WHERE id IN ?;
      |""".trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectForIdsQuery<out T : kotlin.Any>(
      |  @kotlin.jvm.JvmField
      |  val id: kotlin.collections.Collection<foo.Bar?>,
      |  mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(selectForIds, mapper) {
      |  override fun execute(): com.squareup.sqldelight.db.SqlCursor {
      |    val idIndexes = createArguments(count = id.size, offset = 1)
      |    return driver.executeQuery(null, ""${'"'}
      |    |SELECT *
      |    |FROM data
      |    |WHERE id IN ${'$'}idIndexes
      |    ""${'"'}.trimMargin(), id.size) {
      |      id.forEachIndexed { index, id_ ->
      |          bindLong(index + 1, if (id_ == null) null else database.dataAdapter.idAdapter.encode(id_))
      |          }
      |    }
      |  }
      |
      |  override fun toString(): kotlin.String = "Test.sq:selectForIds"
      |}
      |""".trimMargin())
  }

  @Test
  fun `query type generates properly if argument is compared using IS NULL`(dialect: DialectPreset) {
    assumeTrue(dialect !in listOf(DialectPreset.HSQL))
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  token ${dialect.textType} NOT NULL
      |);
      |
      |selectByTokenOrAll:
      |SELECT *
      |FROM data
      |WHERE token = :token OR :token IS NULL;
      |""".trimMargin(), tempFolder, dialectPreset = dialect)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectByTokenOrAllQuery<out T : kotlin.Any>(
      |  @kotlin.jvm.JvmField
      |  val token: kotlin.String?,
      |  mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(selectByTokenOrAll, mapper) {
      |  override fun execute(): com.squareup.sqldelight.db.SqlCursor = driver.executeQuery(null, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE token ${"$"}{ if (token == null) "IS" else "=" } ? OR ? IS NULL
      |  ""${'"'}.trimMargin(), 2) {
      |    bindString(1, token)
      |    bindString(2, token)
      |  }
      |
      |  override fun toString(): kotlin.String = "Test.sq:selectByTokenOrAll"
      |}
      |""".trimMargin())
  }
}
