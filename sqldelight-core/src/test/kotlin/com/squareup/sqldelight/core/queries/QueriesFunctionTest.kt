package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.SelectQueryGenerator
import com.squareup.sqldelight.core.compiler.model.namedQueries
import com.squareup.sqldelight.core.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class QueriesFunctionTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `query function with default result type generates properly`() {
    // This barely tests anything but its easier to verify the codegen works like this.
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo("""
      |fun selectForId(id: kotlin.Long): com.squareup.sqldelight.Query<com.example.Data> = selectForId(id, com.example.Data::Impl)
      """.trimMargin())
  }

  @Test fun `query function with custom result type generates properly`() {
    // This barely tests anything but its easier to verify the codegen works like this.
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
    |fun <T> selectForId(id: kotlin.Long, mapper: (id: kotlin.Long, value: kotlin.String) -> T): com.squareup.sqldelight.Query<T> {
    |    val statement = database.getConnection().prepareStatement(""${'"'}
    |            |SELECT *
    |            |FROM data
    |            |WHERE id = ?
    |            ""${'"'}.trimMargin())
    |    statement.bindLong(1, id)
    |    return SelectForId(id, statement) { resultSet ->
    |        mapper(
    |            resultSet.getLong(0)!!,
    |            resultSet.getString(1)!!
    |        )
    |    }
    |}
    |
      """.trimMargin())
  }

  @Test fun `custom result type query function uses adapter`() {
    // This barely tests anything but its easier to verify the codegen works like this.
    val file = FixtureCompiler.parseSql("""
      |import kotlin.collections.List;
      |
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT AS List NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T> selectForId(id: kotlin.Long, mapper: (id: kotlin.Long, value: kotlin.collections.List) -> T): com.squareup.sqldelight.Query<T> {
      |    val statement = database.getConnection().prepareStatement(""${'"'}
      |            |SELECT *
      |            |FROM data
      |            |WHERE id = ?
      |            ""${'"'}.trimMargin())
      |    statement.bindLong(1, id)
      |    return SelectForId(id, statement) { resultSet ->
      |        mapper(
      |            resultSet.getLong(0)!!,
      |            queryWrapper.dataAdapter.valueAdapter.decode(resultSet.getString(1))!!
      |        )
      |    }
      |}
      |
      """.trimMargin())
  }

  @Test fun `multiple values types are folded into proper result type`() {
    // This barely tests anything but its easier to verify the codegen works like this.
    val file = FixtureCompiler.parseSql("""
      |selectValues:
      |VALUES (1), ('sup');
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())
    assertThat(generator.customResultTypeFunction().toString()).contains("""
      |fun selectValues(): com.squareup.sqldelight.Query<kotlin.String>
      """.trimMargin())
  }

  @Test fun `query with no parameters doesnt subclass Query`() {
    // This barely tests anything but its easier to verify the codegen works like this.
    val file = FixtureCompiler.parseSql("""
      |import kotlin.collections.List;
      |
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT AS List NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T> selectForId(mapper: (id: kotlin.Long, value: kotlin.collections.List) -> T): com.squareup.sqldelight.Query<T> {
      |    val statement = database.getConnection().prepareStatement(""${'"'}
      |            |SELECT *
      |            |FROM data
      |            ""${'"'}.trimMargin())
      |    return com.squareup.sqldelight.Query(statement, selectForId) { resultSet ->
      |        mapper(
      |            resultSet.getLong(0)!!,
      |            queryWrapper.dataAdapter.valueAdapter.decode(resultSet.getString(1))!!
      |        )
      |    }
      |}
      |
      """.trimMargin())
  }
}
