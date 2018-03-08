package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.MutatorQueryGenerator
import com.squareup.sqldelight.core.compiler.SelectQueryGenerator
import com.squareup.sqldelight.core.compiler.model.namedMutators
import com.squareup.sqldelight.core.compiler.model.namedQueries
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SelectQueryFunctionTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `query function with default result type generates properly`() {
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
    |            |WHERE id = ?1
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
      |            |WHERE id = ?1
      |            ""${'"'}.trimMargin())
      |    statement.bindLong(1, id)
      |    return SelectForId(id, statement) { resultSet ->
      |        mapper(
      |            resultSet.getLong(0)!!,
      |            queryWrapper.dataAdapter.valueAdapter.decode(resultSet.getString(1)!!)
      |        )
      |    }
      |}
      |
      """.trimMargin())
  }

  @Test fun `multiple values types are folded into proper result type`() {
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
      |            queryWrapper.dataAdapter.valueAdapter.decode(resultSet.getString(1)!!)
      |        )
      |    }
      |}
      |
      """.trimMargin())
  }

  @Test fun `integer primary key is always exposed as non-null`() {
    // This barely tests anything but its easier to verify the codegen works like this.
    val file = FixtureCompiler.parseSql(
        """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder
    )

    val generator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
        """
      |fun selectData(): com.squareup.sqldelight.Query<kotlin.Long> {
      |    val statement = database.getConnection().prepareStatement(""${'"'}
      |            |SELECT *
      |            |FROM data
      |            ""${'"'}.trimMargin())
      |    return com.squareup.sqldelight.Query(statement, selectData) { resultSet ->
      |        resultSet.getLong(0)!!
      |    }
      |}
      |
      """.trimMargin())
  }

  @Test fun `bind parameter used in IN expression explodes into multiplel query args`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id IN :good AND id NOT IN :bad;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun selectForId(good: kotlin.collections.Collection<kotlin.Long>, bad: kotlin.collections.Collection<kotlin.Long>): com.squareup.sqldelight.Query<kotlin.Long> {
      |    val goodIndexes = good.mapIndexed { index, _ ->
      |            "?${"$"}{ index + 3 }"
      |            }.joinToString(prefix = "(", postfix = ")")
      |    val badIndexes = bad.mapIndexed { index, _ ->
      |            "?${"$"}{ good.size() + index + 3 }"
      |            }.joinToString(prefix = "(", postfix = ")")
      |    val statement = database.getConnection().prepareStatement(""${'"'}
      |            |SELECT *
      |            |FROM data
      |            |WHERE id IN ${"$"}goodIndexes AND id NOT IN ${"$"}badIndexes
      |            ""${'"'}.trimMargin())
      |    good.forEachIndexed { index, good ->
      |            statement.bindLong(index + 3, good)
      |            }
      |    bad.forEachIndexed { index, bad ->
      |            statement.bindLong(good.size() + index + 3, bad)
      |            }
      |    return SelectForId(good, bad, statement) { resultSet ->
      |        resultSet.getLong(0)!!
      |    }
      |}
      |
      """.trimMargin())
  }

  @Test fun `bind parameter inside inner select gets proper type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE some_table (
      |  some_column INTEGER NOT NULL
      |);
      |
      |updateWithInnerSelect:
      |UPDATE some_table
      |SET some_column = (
      |  SELECT CASE WHEN ?1 IS NULL THEN some_column ELSE ?1 END
      |  FROM some_table
      |);
      """.trimMargin(), tempFolder)

    val generator = MutatorQueryGenerator(file.sqliteStatements().namedMutators().first())
    assertThat(generator.function().toString()).isEqualTo("""
      |fun updateWithInnerSelect(some_column: kotlin.Long?): kotlin.Long = updateWithInnerSelect.execute(some_column)
      |""".trimMargin())
  }
}
