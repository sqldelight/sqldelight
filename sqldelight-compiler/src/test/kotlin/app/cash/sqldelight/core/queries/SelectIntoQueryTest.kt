package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.compiler.QueryInterfaceGenerator
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
      |SELECT value, id INTO :FOO, :BAR
      |FROM data
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder,
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectForId(id: kotlin.Long, mapper: (FOO: kotlin.String, BAR: kotlin.Long) -> T): app.cash.sqldelight.Query<T> = SelectForIdQuery(id) { cursor ->
      |  mapper(
      |    cursor.getString(0)!!,
      |    cursor.getLong(1)!!
      |  )
      |}
      |
      """.trimMargin(),
    )

    val dataClassGenerator = QueryInterfaceGenerator(file.namedQueries.first())
    assertThat(dataClassGenerator.kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class SelectForId(
      |  public val FOO: kotlin.String,
      |  public val BAR: kotlin.Long,
      |)
      |
      """.trimMargin(),
    )
  }

  @Test fun `embeddedSQL works with single select`() {
    val file = FixtureCompiler.parseSql(
      """
      |select:
      |SELECT abs(42) INTO :FOO;
      |
      """.trimMargin(),
      tempFolder,
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun select(): app.cash.sqldelight.Query<kotlin.Long> = app.cash.sqldelight.Query(-1626977671, emptyArray(), driver, "Test.sq", "select", "SELECT abs(42)") { cursor ->
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
      |SET :FOO = abs(42);
      |
      """.trimMargin(),
      tempFolder,
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun abs(): app.cash.sqldelight.ExecutableQuery<kotlin.Long> = app.cash.sqldelight.Query(-1951556587, driver, "Test.sq", "abs", "SELECT abs(42)") { cursor ->
      |  cursor.getLong(0)!!
      |}
      |
      """.trimMargin(),
    )

    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun abs(): app.cash.sqldelight.ExecutableQuery<com.example.Abs> = abs { abs ->
      |  com.example.Abs(
      |    abs
      |  )
      |}
      |
      """.trimMargin(),
    )

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class AbsQuery<out T : kotlin.Any>(
      |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
      |) : app.cash.sqldelight.ExecutableQuery<T>(mapper) {
      |  public override fun <R> execute(mapper: (app.cash.sqldelight.db.SqlCursor) -> R): app.cash.sqldelight.db.QueryResult<R> = driver.executeQuery(-1951556587, ""${'"'}SELECT abs(42)""${'"'}, mapper, 0)
      |
      |  public override fun toString(): kotlin.String = "Test.sq:abs"
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `embeddedSQL works with set select`() {
    val file = FixtureCompiler.parseSql(
      """
      |setSelect:
      |SET :FOO = SELECT abs(42);
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
