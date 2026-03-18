package app.cash.sqldelight.core.queries.async

import app.cash.sqldelight.core.compiler.SelectQueryGenerator
import app.cash.sqldelight.core.test.fileContents
import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import com.squareup.burst.BurstJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(BurstJUnit4::class)
class AsyncQueryFunctionTest {
  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun `query function with default result type generates properly`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder,
      generateAsync = true,
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().fileContents()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import kotlin.Long
      |
      |public fun selectForId(id: Long): Query<Data_> = selectForId(id, ::Data_)
      |
      """.trimMargin(),
    )
  }
}
