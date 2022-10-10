package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.compiler.SelectQueryGenerator
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
  fun `postgres INSERT RETURNING works with bind expr ?`() {
    val file = FixtureCompiler.parseSql(
      """
            |CREATE TABLE data (
            |  id INTEGER NOT NULL PRIMARY KEY,
            |  col1 TEXT DEFAULT ''
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

    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
        |public fun insertReturn(data_: com.example.Data_): app.cash.sqldelight.ExecutableQuery<com.example.Data_> = insertReturn(data_) { id, col1 ->
        |  com.example.Data_(
        |    id,
        |    col1
        |  )
        |}
        |
      """.trimMargin(),
    )
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
        |public fun <T : kotlin.Any> insertReturn(data_: com.example.Data_, mapper: (id: kotlin.Int, col1: kotlin.String?) -> T): app.cash.sqldelight.ExecutableQuery<T> = InsertReturnQuery(data_.id, data_.col1) { cursor ->
        |  check(cursor is app.cash.sqldelight.driver.jdbc.JdbcCursor)
        |  mapper(
        |    cursor.getLong(0)!!.toInt(),
        |    cursor.getString(1)
        |  )
        |}
        |
      """.trimMargin()
    )
  }
}