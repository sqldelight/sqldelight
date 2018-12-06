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
      |private inner class SelectForId<out T : kotlin.Any>(private val id: kotlin.Long, mapper: (com.squareup.sqldelight.db.SqlCursor) -> T) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
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
      |private inner class SelectForId<out T : kotlin.Any>(private val id: kotlin.collections.Collection<kotlin.Long>, mapper: (com.squareup.sqldelight.db.SqlCursor) -> T) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
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

    val generator = SelectQueryGenerator(file.namedQueries.first())

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectForId<out T : kotlin.Any>(
      |    private val val_: kotlin.String?,
      |    private val val__: kotlin.String?,
      |    private val val___: kotlin.String?,
      |    private val val____: kotlin.String?,
      |    mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
      |    override fun createStatement(): com.squareup.sqldelight.db.SqlPreparedStatement {
      |        val statement = database.getConnection().prepareStatement(""${'"'}
      |                |SELECT *
      |                |FROM data
      |                |WHERE val ${'$'}{ if (val_ == null) "IS" else "=" } ?1
      |                |AND val ${'$'}{ if (val__ == null) "IS" else "==" } ?2
      |                |AND val ${'$'}{ if (val___ == null) "IS NOT" else "<>" } ?3
      |                |AND val ${'$'}{ if (val____ == null) "IS NOT" else "!=" } ?4
      |                ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT, 4)
      |        statement.bindString(1, val_)
      |        statement.bindString(2, val__)
      |        statement.bindString(3, val___)
      |        statement.bindString(4, val____)
      |        return statement
      |    }
      |}
      |""".trimMargin())
  }
}
