package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.compiler.SelectQueryGenerator
import app.cash.sqldelight.core.test.fileContents
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PgInsertReturningTest {
  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun `postgres INSERT RETURNING * works with bind expr ?`() {
    val file = FixtureCompiler.parseSql(
      """
            |CREATE TABLE data (
            |  id INTEGER NOT NULL PRIMARY KEY,
            |  data TEXT DEFAULT ''
            |);
            |
            |insertReturn:
            |INSERT INTO data
            |VALUES ?
            |RETURNING *;
      """.trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect(),
    )

    val insert = file.namedQueries.first()
    val generator = SelectQueryGenerator(insert)

    assertThat(generator.defaultResultTypeFunction().fileContents()).isEqualTo(
      """
        |package com.example
        |
        |import app.cash.sqldelight.ExecutableQuery
        |
        |public fun insertReturn(data_: Data_): ExecutableQuery<Data_> = insertReturn(data_, ::Data_)
        |
      """.trimMargin(),
    )
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
        |public fun <T : kotlin.Any> insertReturn(data_: com.example.Data_, mapper: (id: kotlin.Int, data_: kotlin.String?) -> T): app.cash.sqldelight.ExecutableQuery<T> = InsertReturnQuery(data_) { cursor ->
        |  check(cursor is app.cash.sqldelight.driver.jdbc.JdbcCursor)
        |  mapper(
        |    cursor.getInt(0)!!,
        |    cursor.getString(1)
        |  )
        |}
        |
      """.trimMargin(),
    )
  }

  @Test
  fun `postgres INSERT RETURNING works with bind expr ? and returning columns`() {
    val file = FixtureCompiler.parseSql(
      """
            |CREATE TABLE data (
            |  id INTEGER NOT NULL PRIMARY KEY,
            |  data TEXT DEFAULT ''
            |);
            |
            |insertReturn:
            |INSERT INTO data
            |VALUES ?
            |RETURNING data, id;
      """.trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect(),
    )

    val insert = file.namedQueries.first()
    val generator = SelectQueryGenerator(insert)

    assertThat(generator.defaultResultTypeFunction().fileContents()).isEqualTo(
      """
        |package com.example
        |
        |import app.cash.sqldelight.ExecutableQuery
        |
        |public fun insertReturn(data_: Data_): ExecutableQuery<InsertReturn> = insertReturn(data_, ::InsertReturn)
        |
      """.trimMargin(),
    )
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
        |public fun <T : kotlin.Any> insertReturn(data_: com.example.Data_, mapper: (data_: kotlin.String?, id: kotlin.Int) -> T): app.cash.sqldelight.ExecutableQuery<T> = InsertReturnQuery(data_) { cursor ->
        |  check(cursor is app.cash.sqldelight.driver.jdbc.JdbcCursor)
        |  mapper(
        |    cursor.getString(0),
        |    cursor.getInt(1)!!
        |  )
        |}
        |
      """.trimMargin(),
    )
  }
}
