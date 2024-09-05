package app.cash.sqldelight.core.queries.async

import app.cash.sqldelight.core.compiler.MutatorQueryGenerator
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.withUnderscores
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AsyncMutatorQueryTypeTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `type is generated properly for no result set changes`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.Int PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<kotlin.String>
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(),
      tempFolder,
      generateAsync = true,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public suspend fun insertData(id: kotlin.Int?, value_: kotlin.collections.List<kotlin.String>?) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        bindLong(0, id?.let { data_Adapter.idAdapter.encode(it) })
      |        bindString(1, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |      }.await()
      |  notifyQueries(1_642_410_240) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `type is generated properly for result set changes in same file`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.Int PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<kotlin.String>
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
      generateAsync = true,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public suspend fun insertData(id: kotlin.Int?, value_: kotlin.collections.List<kotlin.String>?) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        bindLong(0, id?.let { data_Adapter.idAdapter.encode(it) })
      |        bindString(1, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |      }.await()
      |  notifyQueries(${mutator.id.withUnderscores}) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `type is generated properly for result set changes in different file`() {
    FixtureCompiler.writeSql(
      """
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder,
      fileName = "OtherData.sq",
    )

    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.Int PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List<kotlin.String>
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(),
      tempFolder,
      fileName = "Data.sq",
      generateAsync = true,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public suspend fun insertData(id: kotlin.Int?, value_: kotlin.collections.List<kotlin.String>?) {
      |  driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO data
      |      |VALUES (?, ?)
      |      ""${'"'}.trimMargin(), 2) {
      |        bindLong(0, id?.let { data_Adapter.idAdapter.encode(it) })
      |        bindString(1, value_?.let { data_Adapter.value_Adapter.encode(it) })
      |      }.await()
      |  notifyQueries(${mutator.id.withUnderscores}) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `Async Postgresql INSERT VALUES use correct bind parameter with the table data class`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES ?;
      """.trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect(),
      fileName = "Data.sq",
      generateAsync = true,
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public suspend fun insertData(data_: com.example.Data_) {
      |  driver.execute(208_179_736, ""${'"'}
      |      |INSERT INTO data (id, value)
      |      |VALUES (${'$'}1, ${'$'}2)
      |      ""${'"'}.trimMargin(), 2) {
      |        check(this is app.cash.sqldelight.driver.r2dbc.R2dbcPreparedStatement)
      |        bindInt(0, data_.id)
      |        bindString(1, data_.value_)
      |      }.await()
      |  notifyQueries(208_179_736) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }
}
