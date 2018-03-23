package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.SelectQueryGenerator
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

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo("""
      |fun selectForId(id: kotlin.Long): com.squareup.sqldelight.Query<com.example.Data> = selectForId(id, com.example.Data::Impl)
      |""".trimMargin())
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

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
    |fun <T> selectForId(id: kotlin.Long, mapper: (id: kotlin.Long, value: kotlin.String) -> T): com.squareup.sqldelight.Query<T> {
    |    val statement = database.getConnection().prepareStatement(""${'"'}
    |            |SELECT *
    |            |FROM data
    |            |WHERE id = ?1
    |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT)
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

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T> selectForId(id: kotlin.Long, mapper: (id: kotlin.Long, value: kotlin.collections.List) -> T): com.squareup.sqldelight.Query<T> {
      |    val statement = database.getConnection().prepareStatement(""${'"'}
      |            |SELECT *
      |            |FROM data
      |            |WHERE id = ?1
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT)
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

    val generator = SelectQueryGenerator(file.namedQueries.first())
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

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T> selectForId(mapper: (id: kotlin.Long, value: kotlin.collections.List) -> T): com.squareup.sqldelight.Query<T> {
      |    val statement = database.getConnection().prepareStatement(""${'"'}
      |            |SELECT *
      |            |FROM data
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT)
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

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
        """
      |fun selectData(): com.squareup.sqldelight.Query<kotlin.Long> {
      |    val statement = database.getConnection().prepareStatement(""${'"'}
      |            |SELECT *
      |            |FROM data
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT)
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

    val generator = SelectQueryGenerator(file.namedQueries.first())
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
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT)
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

  @Test fun `limit and offset bind expressions gets proper types`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  some_column INTEGER NOT NULL,
      |  some_column2 INTEGER NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM data
      |WHERE EXISTS (SELECT * FROM data LIMIT :minimum OFFSET :offset)
      |LIMIT :minimum;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo("""
      |fun someSelect(minimum: kotlin.Long, offset: kotlin.Long): com.squareup.sqldelight.Query<com.example.Data> = someSelect(minimum, offset, com.example.Data::Impl)
      |""".trimMargin())
  }

  @Test fun `boolean column mapper from result set properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  value INTEGER AS Boolean
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T> selectData(mapper: (id: kotlin.Long, value: kotlin.Boolean?) -> T): com.squareup.sqldelight.Query<T> {
      |    val statement = database.getConnection().prepareStatement(""${'"'}
      |            |SELECT *
      |            |FROM data
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT)
      |    return com.squareup.sqldelight.Query(statement, selectData) { resultSet ->
      |        mapper(
      |            resultSet.getLong(0)!!,
      |            resultSet.getLong(1)?.let { it == 1L }
      |        )
      |    }
      |}
      |
      """.trimMargin())
  }

  @Test fun `named bind arg can be reused`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE person (
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  first_name TEXT NOT NULL,
      |  last_name TEXT NOT NULL
      |);
      |
      |equivalentNamesNamed:
      |SELECT *
      |FROM person
      |WHERE first_name = :name AND last_name = :name;
      """.trimMargin(), tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T> equivalentNamesNamed(name: kotlin.String, mapper: (
      |        _id: kotlin.Long,
      |        first_name: kotlin.String,
      |        last_name: kotlin.String
      |) -> T): com.squareup.sqldelight.Query<T> {
      |    val statement = database.getConnection().prepareStatement(""${'"'}
      |            |SELECT *
      |            |FROM person
      |            |WHERE first_name = ?1 AND last_name = ?1
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT)
      |    statement.bindString(1, name)
      |    return EquivalentNamesNamed(name, statement) { resultSet ->
      |        mapper(
      |            resultSet.getLong(0)!!,
      |            resultSet.getString(1)!!,
      |            resultSet.getString(2)!!
      |        )
      |    }
      |}
      |
      """.trimMargin())
  }

  @Test fun `real is exposed properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  value REAL NOT NULL
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun selectData(): com.squareup.sqldelight.Query<kotlin.Double> {
      |    val statement = database.getConnection().prepareStatement(""${'"'}
      |            |SELECT *
      |            |FROM data
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT)
      |    return com.squareup.sqldelight.Query(statement, selectData) { resultSet ->
      |        resultSet.getDouble(0)!!
      |    }
      |}
      |
      """.trimMargin())
  }

  @Test fun `blob is exposed properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  value BLOB NOT NULL
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun selectData(): com.squareup.sqldelight.Query<kotlin.ByteArray> {
      |    val statement = database.getConnection().prepareStatement(""${'"'}
      |            |SELECT *
      |            |FROM data
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT)
      |    return com.squareup.sqldelight.Query(statement, selectData) { resultSet ->
      |        resultSet.getBytes(0)!!
      |    }
      |}
      |
      """.trimMargin())
  }

  @Test fun `null is exposed properly`() {
    val file = FixtureCompiler.parseSql("""
      |selectData:
      |SELECT NULL;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun selectData(): com.squareup.sqldelight.Query<java.lang.Void?> {
      |    val statement = database.getConnection().prepareStatement("SELECT NULL", com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT)
      |    return com.squareup.sqldelight.Query(statement, selectData) { resultSet ->
      |        null
      |    }
      |}
      |
      """.trimMargin())
  }

  @Test fun `non null boolean is exposed properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  value INTEGER AS Boolean NOT NULL
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun selectData(): com.squareup.sqldelight.Query<kotlin.Boolean> {
      |    val statement = database.getConnection().prepareStatement(""${'"'}
      |            |SELECT *
      |            |FROM data
      |            ""${'"'}.trimMargin(), com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT)
      |    return com.squareup.sqldelight.Query(statement, selectData) { resultSet ->
      |        resultSet.getLong(0)!! == 1L
      |    }
      |}
      |
      """.trimMargin())
  }

  @Test fun `query returns custom query type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  value INTEGER,
      |  value2 INTEGER
      |);
      |
      |selectData:
      |SELECT coalesce(value, value2), value, value2
      |FROM data;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo("""
      |fun selectData(): com.squareup.sqldelight.Query<com.example.SelectData> = selectData(com.example.SelectData::Impl)
      |""".trimMargin())
  }
}
