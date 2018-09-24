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
      |private inner class SelectForId<out T : kotlin.Any>(private val id: kotlin.Long, mapper: (com.squareup.sqldelight.db.SqlResultSet) -> T) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
      |    override fun createStatement(): com.squareup.sqldelight.db.SqlPreparedStatement {
      |        val statement = database.getConnection().prepareStatement(""${'"'}
      |                |SELECT *
      |                |FROM data
      |                |WHERE id = ?1
      |                ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT, 1)
      |        statement.bindLong(1, id)
      |        return statement
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
      |private inner class SelectForId<out T : kotlin.Any>(private val id: kotlin.collections.Collection<kotlin.Long>, mapper: (com.squareup.sqldelight.db.SqlResultSet) -> T) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
      |    override fun createStatement(): com.squareup.sqldelight.db.SqlPreparedStatement {
      |        val idIndexes = createArguments(count = id.size, offset = 2)
      |        val statement = database.getConnection().prepareStatement(""${'"'}
      |                |SELECT *
      |                |FROM data
      |                |WHERE id IN ${'$'}idIndexes
      |                ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT, id.size)
      |        id.forEachIndexed { index, id ->
      |                statement.bindLong(index + 2, id)
      |                }
      |        return statement
      |    }
      |}
      |""".trimMargin())
  }
}
