package app.cash.sqldelight.core.queries.async

import app.cash.sqldelight.core.compiler.MutatorQueryGenerator
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
}
