package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.compiler.MutatorQueryGenerator
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.withUnderscores
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PgInsertOnConflictTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test
  fun `postgres INSERT DO UPDATE works with 1 column`() {
    val file = FixtureCompiler.parseSql(
      """
            |CREATE TABLE data (
            |  id INTEGER NOT NULL PRIMARY KEY,
            |  col1 TEXT DEFAULT ''
            |);
            |
            |upsertCols:
            |INSERT INTO data
            |VALUES (:id, :c1)
            |ON CONFLICT (id) DO UPDATE SET col1 = :c1;
      """.trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect(),
    )

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)

    assertThat(generator.function().toString()).isEqualTo(
      """
            |public fun upsertCols(id: kotlin.Int?, c1: kotlin.String?) {
            |  driver.execute(${insert.id.withUnderscores}, ""${'"'}
            |      |INSERT INTO data
            |      |VALUES (?, ?)
            |      |ON CONFLICT (id) DO UPDATE SET col1 = ?
            |      ""${'"'}.trimMargin(), 3) {
            |        check(this is app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement)
            |        bindInt(0, id)
            |        bindString(1, c1)
            |        bindString(2, c1)
            |      }
            |  notifyQueries(${insert.id.withUnderscores}) { emit ->
            |    emit("data")
            |  }
            |}
            |
      """.trimMargin(),
    )
  }

  @Test
  fun `postgres INSERT DO UPDATE works with 2 columns`() {
    val file = FixtureCompiler.parseSql(
      """
            |CREATE TABLE data (
            |  id INTEGER NOT NULL PRIMARY KEY,
            |  col1 TEXT DEFAULT '',
            |  col2 TEXT DEFAULT '',
            |  col3 TEXT DEFAULT ''
            |);
            |
            |upsertCols:
            |INSERT INTO data
            |VALUES (:id, :c1, :c2, :c3)
            |ON CONFLICT (id) DO UPDATE SET col1 = :c1, col2 = :c2;
      """.trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect(),
    )

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)

    assertThat(generator.function().toString()).isEqualTo(
      """
            |public fun upsertCols(
            |  id: kotlin.Int?,
            |  c1: kotlin.String?,
            |  c2: kotlin.String?,
            |  c3: kotlin.String?,
            |) {
            |  driver.execute(${insert.id.withUnderscores}, ""${'"'}
            |      |INSERT INTO data
            |      |VALUES (?, ?, ?, ?)
            |      |ON CONFLICT (id) DO UPDATE SET col1 = ?, col2 = ?
            |      ""${'"'}.trimMargin(), 6) {
            |        check(this is app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement)
            |        bindInt(0, id)
            |        bindString(1, c1)
            |        bindString(2, c2)
            |        bindString(3, c3)
            |        bindString(4, c1)
            |        bindString(5, c2)
            |      }
            |  notifyQueries(${insert.id.withUnderscores}) { emit ->
            |    emit("data")
            |  }
            |}
            |
      """.trimMargin(),
    )
  }

  @Test
  fun `postgres INSERT DO UPDATE works with 3 columns`() {
    val file = FixtureCompiler.parseSql(
      """
            |CREATE TABLE data (
            |  id INTEGER NOT NULL PRIMARY KEY,
            |  col1 TEXT DEFAULT '',
            |  col2 TEXT DEFAULT '',
            |  col3 TEXT DEFAULT ''
            |);
            |
            |upsertCols:
            |INSERT INTO data
            |VALUES (:id, :c1, :c2, :c3)
            |ON CONFLICT (id) DO UPDATE SET col1 = :c1, col2 = :c2, col3 = :c3;
      """.trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect(),
    )

    val insert = file.namedMutators.first()
    val generator = MutatorQueryGenerator(insert)

    assertThat(generator.function().toString()).isEqualTo(
      """
            |public fun upsertCols(
            |  id: kotlin.Int?,
            |  c1: kotlin.String?,
            |  c2: kotlin.String?,
            |  c3: kotlin.String?,
            |) {
            |  driver.execute(${insert.id.withUnderscores}, ""${'"'}
            |      |INSERT INTO data
            |      |VALUES (?, ?, ?, ?)
            |      |ON CONFLICT (id) DO UPDATE SET col1 = ?, col2 = ?, col3 = ?
            |      ""${'"'}.trimMargin(), 7) {
            |        check(this is app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement)
            |        bindInt(0, id)
            |        bindString(1, c1)
            |        bindString(2, c2)
            |        bindString(3, c3)
            |        bindString(4, c1)
            |        bindString(5, c2)
            |        bindString(6, c3)
            |      }
            |  notifyQueries(${insert.id.withUnderscores}) { emit ->
            |    emit("data")
            |  }
            |}
            |
      """.trimMargin(),
    )
  }
}
