package app.cash.sqldelight.core.queries.async

import app.cash.sqldelight.core.compiler.SelectQueryGenerator
import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth
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
    Truth.assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun selectForId(id: kotlin.Long): app.cash.sqldelight.Query<com.example.Data_> = selectForId(id) { id_, value_ ->
      |  com.example.Data_(
      |    id_,
      |    value_
      |  )
      |}
      |
      """.trimMargin(),
    )
  }
}
