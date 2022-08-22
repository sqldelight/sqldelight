package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.compiler.SelectQueryGenerator
import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import com.squareup.burst.BurstJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(BurstJUnit4::class)
class SelectIntoQueryTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `embeddedSQL works with table definition`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT value, id INTO ?, ?
      |FROM data
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder,
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectForId(id: kotlin.Long, mapper: (value_: kotlin.String, id: kotlin.Long) -> T): app.cash.sqldelight.Query<T> = SelectForIdQuery(id) { cursor ->
      |  mapper(
      |    cursor.getString(0)!!,
      |    cursor.getLong(1)!!
      |  )
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `embeddedSQL works with single select`() {
    val file = FixtureCompiler.parseSql(
      """
      |select:
      |SELECT abs(42) INTO ?;
      |
      """.trimMargin(),
      tempFolder,
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun select(): app.cash.sqldelight.Query<kotlin.Long> = app.cash.sqldelight.Query(-1626977671, emptyArray(), driver, "Test.sq", "select", "SELECT abs(42) INTO ?") { cursor ->
      |  cursor.getLong(0)!!
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `embeddedSQL works with set`() {
    val file = FixtureCompiler.parseSql(
      """
      |abs:
      |SET ? = abs(42);
      |
      """.trimMargin(),
      tempFolder,
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun abs(): app.cash.sqldelight.Query<kotlin.Long> = app.cash.sqldelight.Query(-1951556587, driver, "Test.sq", "abs", "SELECT abs(42)") { cursor ->
      |  cursor.getLong(0)!!
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `embeddedSQL works with set select`() {
    val file = FixtureCompiler.parseSql(
      """
      |setSelect:
      |SET ? = SELECT abs(42);
      |
      """.trimMargin(),
      tempFolder,
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun setSelect(): app.cash.sqldelight.Query<kotlin.Long> = app.cash.sqldelight.Query(1705584993, emptyArray(), driver, "Test.sq", "setSelect", "SELECT abs(42)") { cursor ->
      |  cursor.getLong(0)!!
      |}
      |
      """.trimMargin(),
    )
  }
}
