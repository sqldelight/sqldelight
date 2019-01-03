package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.SelectQueryGenerator
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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
      |private inner class SelectForId<out T : kotlin.Any>(private val id: kotlin.Long, mapper: (com.squareup.sqldelight.db.SqlCursor) -> T) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
      |    override fun execute(): com.squareup.sqldelight.db.SqlCursor = database.executeQuery(${query.id}, ""${'"'}
      |    |SELECT *
      |    |FROM data
      |    |WHERE id = ?1
      |    ""${'"'}.trimMargin(), 1) {
      |        bindLong(1, id)
      |    }
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
      |private inner class Select<out T : kotlin.Any>(
      |    private val value: kotlin.String,
      |    private val id: kotlin.Long,
      |    mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(select, mapper) {
      |    override fun execute(): com.squareup.sqldelight.db.SqlCursor = database.executeQuery(${query.id}, ""${'"'}
      |    |SELECT *
      |    |FROM data
      |    |WHERE id = ?2
      |    |AND value = ?1
      |    ""${'"'}.trimMargin(), 2) {
      |        bindLong(2, id)
      |        bindString(1, value)
      |    }
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
      |private inner class SelectForId<out T : kotlin.Any>(private val id: kotlin.collections.Collection<kotlin.Long>, mapper: (com.squareup.sqldelight.db.SqlCursor) -> T) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
      |    override fun execute(): com.squareup.sqldelight.db.SqlCursor {
      |        val idIndexes = createArguments(count = id.size, offset = 2)
      |        return database.executeQuery(null, ""${'"'}
      |        |SELECT *
      |        |FROM data
      |        |WHERE id IN ${"$"}idIndexes
      |        ""${'"'}.trimMargin(), id.size) {
      |            id.forEachIndexed { index, id ->
      |                    bindLong(index + 2, id)
      |                    }
      |        }
      |    }
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
       |private inner class Select_news_list<out T : kotlin.Any>(private val userId: kotlin.String?, mapper: (com.squareup.sqldelight.db.SqlCursor) -> T) : com.squareup.sqldelight.Query<T>(select_news_list, mapper) {
       |    override fun execute(): com.squareup.sqldelight.db.SqlCursor = database.executeQuery(null, ""${'"'}SELECT * FROM socialFeedItem WHERE message IS NOT NULL AND userId ${"$"}{ if (userId == null) "IS" else "=" } ?1 ORDER BY datetime(creation_time) DESC""${'"'}, 1) {
       |        bindString(1, userId)
       |    }
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
      |private inner class SelectForId<out T : kotlin.Any>(
      |    private val val_: kotlin.String?,
      |    private val val__: kotlin.String?,
      |    private val val___: kotlin.String?,
      |    private val val____: kotlin.String?,
      |    mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
      |    override fun execute(): com.squareup.sqldelight.db.SqlCursor = database.executeQuery(null, ""${'"'}
      |    |SELECT *
      |    |FROM data
      |    |WHERE val ${"$"}{ if (val_ == null) "IS" else "=" } ?1
      |    |AND val ${"$"}{ if (val__ == null) "IS" else "==" } ?2
      |    |AND val ${"$"}{ if (val___ == null) "IS NOT" else "<>" } ?3
      |    |AND val ${"$"}{ if (val____ == null) "IS NOT" else "!=" } ?4
      |    ""${'"'}.trimMargin(), 4) {
      |        bindString(1, val_)
      |        bindString(2, val__)
      |        bindString(3, val___)
      |        bindString(4, val____)
      |    }
      |}
      |""".trimMargin())
  }
}
