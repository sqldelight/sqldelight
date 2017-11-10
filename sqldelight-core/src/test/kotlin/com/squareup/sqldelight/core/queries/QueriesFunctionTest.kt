package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.SelectQueryGenerator
import com.squareup.sqldelight.core.compiler.namedQueries
import com.squareup.sqldelight.core.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class QueriesFunctionTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `query function with default result type generates properly`() {
    // This barely tests anything but its easier to verify the codegen works like this.
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  _id INTEGER NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE _id = ?;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo("""
      |fun selectForId(_id: kotlin.Long): com.squareup.sqldelight.Query<com.example.Data> = selectForId(_id, com.example.Data::Impl)
      """.trimMargin())
  }
}
