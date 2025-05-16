package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.compiler.SelectQueryGenerator
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IsDistinctFromTest {
  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun `postgres IS DISTINCT FROM works with bind expr ?`() {
    val file = FixtureCompiler.parseSql(
      """
            |CREATE TABLE data (
            |  id INTEGER NOT NULL PRIMARY KEY,
            |  data TEXT DEFAULT ''
            |);
            |
            |selectOthers:
            |SELECT * FROM data WHERE id IS DISTINCT FROM ?;
      """.trimMargin(),
      tempFolder,
    )

    val insert = file.namedQueries.first()
    val generator = SelectQueryGenerator(insert)

    assertThat(generator.querySubtype().toString()).isEqualTo(
        """
        |private inner class SelectOthersQuery<out T : kotlin.Any>(
        |  public val id: kotlin.Long,
        |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
        |) : app.cash.sqldelight.Query<T>(mapper) {
        |  public override fun addListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
        |    driver.addListener(listener, arrayOf("data"))
        |  }
        |
        |  public override fun removeListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
        |    driver.removeListener(listener, arrayOf("data"))
        |  }
        |
        |  public override fun <R> execute(mapper: (app.cash.sqldelight.db.SqlCursor) -> R): app.cash.sqldelight.db.QueryResult<R> = driver.executeQuery(-572843204, ""${'"'}SELECT * FROM data WHERE id IS DISTINCT FROM ?""${'"'}, mapper, 1) {
        |    bindLong(0, id)
        |  }
        |
        |  public override fun toString(): kotlin.String = "Test.sq:selectOthers"
        |}
        |
        """.trimMargin(),
    )
  }

  @Test
  fun `postgres IS DISTINCT NOT FROM works with bind expr ?`() {
    val file = FixtureCompiler.parseSql(
      """
            |CREATE TABLE data (
            |  id INTEGER NOT NULL PRIMARY KEY,
            |  data TEXT DEFAULT ''
            |);
            |
            |selectIt:
            |SELECT * FROM data WHERE id IS NOT DISTINCT FROM ?;
      """.trimMargin(),
      tempFolder,
    )

    val insert = file.namedQueries.first()
    val generator = SelectQueryGenerator(insert)

    assertThat(generator.querySubtype().toString()).isEqualTo(
        """
        |private inner class SelectItQuery<out T : kotlin.Any>(
        |  public val id: kotlin.Long,
        |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
        |) : app.cash.sqldelight.Query<T>(mapper) {
        |  public override fun addListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
        |    driver.addListener(listener, arrayOf("data"))
        |  }
        |
        |  public override fun removeListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
        |    driver.removeListener(listener, arrayOf("data"))
        |  }
        |
        |  public override fun <R> execute(mapper: (app.cash.sqldelight.db.SqlCursor) -> R): app.cash.sqldelight.db.QueryResult<R> = driver.executeQuery(-157443708, ""${'"'}SELECT * FROM data WHERE id IS NOT DISTINCT FROM ?""${'"'}, mapper, 1) {
        |    bindLong(0, id)
        |  }
        |
        |  public override fun toString(): kotlin.String = "Test.sq:selectIt"
        |}
        |
        """.trimMargin(),
    )
  }
}
