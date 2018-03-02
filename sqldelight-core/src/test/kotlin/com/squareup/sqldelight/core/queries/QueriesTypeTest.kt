package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.SelectQueryGenerator
import com.squareup.sqldelight.core.compiler.namedQueries
import com.squareup.sqldelight.core.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class QueriesTypeTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `query type generates properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER NOT NULL PRIMARY KEY
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE _id = ?;
      |""".trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectForId<out T>(
      |        private val _id: kotlin.Long,
      |        statement: com.squareup.sqldelight.db.SqlPreparedStatement,
      |        mapper: (com.squareup.sqldelight.db.SqlResultSet) -> T
      |) : com.squareup.sqldelight.Query<T>(statement, selectForId, mapper) {
      |    fun dirtied(_id: kotlin.Long): kotlin.Boolean = true
      |}
      |""".trimMargin())
  }
}