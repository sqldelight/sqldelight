package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.compiler.QueryInterfaceGenerator
import app.cash.sqldelight.core.compiler.SelectQueryGenerator
import app.cash.sqldelight.test.util.FixtureCompiler
import app.softwork.sqldelight.db2dialect.Db2Dialect
import com.google.common.truth.Truth.assertThat
import com.squareup.burst.BurstJUnit4
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(BurstJUnit4::class)
class SelectIntoQueryTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `embeddedSQL works with table definition`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value VARCHAR(8) NOT NULL
      |);
      |
      |selectForId:
      |SELECT value, id INTO :FOO, :BAR
      |FROM data
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder,
      dialect = Db2Dialect(),
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectForId(mapper: (FOO: kotlin.String, BAR: kotlin.Int) -> T): app.cash.sqldelight.Query<T> = app.cash.sqldelight.Query(-304025397, arrayOf("data"), driver, "Test.sq", "selectForId", ""${'"'}
      ||SELECT value, id
      ||FROM data
      ||WHERE id = ?
      |""${'"'}.trimMargin()) { cursor ->
      |  check(cursor is app.cash.sqldelight.driver.jdbc.JdbcCursor)
      |  mapper(
      |    cursor.getString(0)!!,
      |    cursor.getLong(1)!!.toInt()
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
      |  public val BAR: kotlin.Int,
      |)
      |
      """.trimMargin(),
    )
  }

  @Test fun `embeddedSQL works with single select`() {
    val file = FixtureCompiler.parseSql(
      """
      |select:
      |SELECT abs(42) INTO :FOO FROM SYSIBM.SYSDUMMY1;
      |
      """.trimMargin(),
      tempFolder,
      dialect = Db2Dialect(),
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun select(): app.cash.sqldelight.Query<kotlin.Long> = app.cash.sqldelight.Query(-1626977671, arrayOf("SYSDUMMY1"), driver, "Test.sq", "select", "SELECT abs(42) FROM SYSIBM.SYSDUMMY1") { cursor ->
      |  check(cursor is app.cash.sqldelight.driver.jdbc.JdbcCursor)
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
      dialect = Db2Dialect(),
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun abs(): app.cash.sqldelight.ExecutableQuery<kotlin.Long> = app.cash.sqldelight.Query(-1951556587, driver, "Test.sq", "abs", "SELECT abs(42) FROM SYSIBM.SYSDUMMY1") { cursor ->
      |  check(cursor is app.cash.sqldelight.driver.jdbc.JdbcCursor)
      |  cursor.getLong(0)!!
      |}
      |
      """.trimMargin(),
    )

    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |public fun abs(): app.cash.sqldelight.ExecutableQuery<com.example.Abs> = abs { FOO ->
      |  com.example.Abs(
      |    FOO
      |  )
      |}
      |
      """.trimMargin(),
    )

    val dataClassGenerator = QueryInterfaceGenerator(file.namedQueries.first())
    assertThat(dataClassGenerator.kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class Abs(
      |  public val FOO: kotlin.Long,
      |)
      |
      """.trimMargin(),
    )

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class AbsQuery<out T : kotlin.Any>(
      |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
      |) : app.cash.sqldelight.ExecutableQuery<T>(mapper) {
      |  public override fun <R> execute(mapper: (app.cash.sqldelight.db.SqlCursor) -> R): app.cash.sqldelight.db.QueryResult<R> = driver.executeQuery(-1951556587, ""${'"'}SELECT abs(42) FROM SYSIBM.SYSDUMMY1""${'"'}, mapper, 0)
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
      |SET :FOO = SELECT abs(42) FROM SYSIBM.SYSDUMMY1;
      |
      """.trimMargin(),
      tempFolder,
      dialect = Db2Dialect(),
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun setSelect(): app.cash.sqldelight.Query<kotlin.Long> = app.cash.sqldelight.Query(1705584993, arrayOf("SYSDUMMY1"), driver, "Test.sq", "setSelect", "SELECT abs(42) FROM SYSIBM.SYSDUMMY1") { cursor ->
      |  check(cursor is app.cash.sqldelight.driver.jdbc.JdbcCursor)
      |  cursor.getLong(0)!!
      |}
      |
      """.trimMargin(),
    )

    val dataClassGenerator = QueryInterfaceGenerator(file.namedQueries.first())
    assertThat(dataClassGenerator.kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class SetSelect(
      |  public val FOO: kotlin.Long,
      |)
      |
      """.trimMargin(),
    )
  }
}
