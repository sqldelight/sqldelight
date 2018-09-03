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

    val generator = SelectQueryGenerator(file.namedQueries.first())

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectForId<out T : kotlin.Any>(
      |    private val id: kotlin.Long,
      |    statement: com.squareup.sqldelight.db.SqlPreparedStatement,
      |    mapper: (com.squareup.sqldelight.db.SqlResultSet) -> T
      |) : com.squareup.sqldelight.Query<T>(statement, selectForId, mapper)
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
      |private inner class SelectForId<out T : kotlin.Any>(
      |    private val id: kotlin.collections.Collection<kotlin.Long>,
      |    statement: com.squareup.sqldelight.db.SqlPreparedStatement,
      |    mapper: (com.squareup.sqldelight.db.SqlResultSet) -> T
      |) : com.squareup.sqldelight.Query<T>(statement, selectForId, mapper)
      |""".trimMargin())
  }
}
