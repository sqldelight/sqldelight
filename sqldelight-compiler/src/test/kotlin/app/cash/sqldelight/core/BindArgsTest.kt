package app.cash.sqldelight.core

import app.cash.sqldelight.core.lang.argumentType
import app.cash.sqldelight.core.lang.types.typeResolver
import app.cash.sqldelight.core.lang.util.argumentType
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.core.lang.util.isArrayParameter
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.dialects.sqlite_3_24.SqliteDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.asClassName
import java.time.Instant
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BindArgsTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `bind arg inherit name from column`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE data.id = ?;
      """.trimMargin(),
      tempFolder,
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    val bindArgType = file.findChildrenOfType<SqlBindExpr>().first().argumentType()
    assertThat(bindArgType.dialectType).isEqualTo(PrimitiveType.INTEGER)
    assertThat(bindArgType.javaType).isEqualTo(List::class.asClassName())
    assertThat(bindArgType.name).isEqualTo("id")
    assertThat(bindArgType.column).isSameInstanceAs(column)
  }

  @Test fun `bind args inherit name from alias`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |CREATE VIEW data_aliased AS
      |SELECT id AS data_id
      |FROM data;
      |
      |selectForId:
      |SELECT *
      |FROM data_aliased
      |WHERE data_id = ?;
      """.trimMargin(),
      tempFolder,
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    val bindArgType = file.findChildrenOfType<SqlBindExpr>().first().argumentType()
    assertThat(bindArgType.dialectType).isEqualTo(PrimitiveType.INTEGER)
    assertThat(bindArgType.javaType).isEqualTo(List::class.asClassName())
    assertThat(bindArgType.name).isEqualTo("data_id")
    assertThat(bindArgType.column).isSameInstanceAs(column)
  }

  @Test fun `bind args inherit names in insert statements`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |insertData:
      |INSERT INTO data (id)
      |VALUES (?), (?);
      """.trimMargin(),
      tempFolder,
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(PrimitiveType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("id")
      assertThat(it.column).isSameInstanceAs(column)
    }
  }

  @Test fun `bind args inherit names in default insert statements`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?), (?);
      """.trimMargin(),
      tempFolder,
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(PrimitiveType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("id")
      assertThat(it.column).isSameInstanceAs(column)
    }
  }

  @Test fun `bind args in compound select inherit type from compounded query`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM data
      |UNION
      |VALUES (?);
      """.trimMargin(),
      tempFolder,
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(PrimitiveType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("id")
      assertThat(it.column).isSameInstanceAs(column)
    }
  }

  @Test fun `bind args in parenthesis in compound select infers type from compounded query`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM data
      |UNION
      |VALUES (((?)));
      """.trimMargin(),
      tempFolder,
    )

    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(PrimitiveType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("id")
      assertThat(it.column).isNotNull()
    }
  }

  @Test fun `bind args for update statements inherit type from column`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someUpdate:
      |UPDATE data
      |SET id = ?
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder,
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(PrimitiveType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("id")
      assertThat(it.column).isSameInstanceAs(column)
    }
  }

  @Test fun `bind args for upsert do update statements inherit type from column`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY NOT NULL,
      |  list INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someUpsert:
      |INSERT INTO data(
      |  id, list
      |)
      |VALUES(
      |  :id, :list
      |)
      |ON CONFLICT(
      |  id
      |)
      |DO UPDATE SET
      |  list = :list;
      """.trimMargin(),
      tempFolder,
      dialect = SqliteDialect(),
    )

    val (idColumn, listColumn) = file.findChildrenOfType<SqlColumnDef>().toList()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      when (it.name) {
        "id" -> {
          assertThat(it.dialectType).isEqualTo(PrimitiveType.INTEGER)
          assertThat(it.javaType).isEqualTo(Long::class.asClassName())
          assertThat(it.name).isEqualTo("id")
          assertThat(it.column).isSameInstanceAs(idColumn)
        }

        "list" -> {
          assertThat(it.dialectType).isEqualTo(PrimitiveType.INTEGER)
          assertThat(it.javaType).isEqualTo(List::class.asClassName())
          assertThat(it.name).isEqualTo("list")
          assertThat(it.column).isSameInstanceAs(listColumn)
        }
      }
    }
  }

  @Test fun `bind args inherit alias name`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM (
      |  SELECT id AS some_alias
      |  FROM data
      |)
      |WHERE some_alias = ?;
      """.trimMargin(),
      tempFolder,
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(PrimitiveType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("some_alias")
      assertThat(it.column).isSameInstanceAs(column)
    }
  }

  @Test fun `bind args for in statement inherit column name`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER AS kotlin.collections.List NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM data
      |WHERE id IN ?;
      """.trimMargin(),
      tempFolder,
    )

    val column = file.findChildrenOfType<SqlColumnDef>().first()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.forEach {
      assertThat(it.dialectType).isEqualTo(PrimitiveType.INTEGER)
      assertThat(it.javaType).isEqualTo(List::class.asClassName())
      assertThat(it.name).isEqualTo("id")
      assertThat(it.column).isSameInstanceAs(column)
      assertThat(it.bindArg!!.isArrayParameter()).isTrue()
    }
  }

  @Test fun `bind arg kotlin type cannot be inferred with ambiguous sql parameter types`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE dummy(
      |  foo INTEGER
      |);
      |
      |maxSupportsManySqlTypes:
      |SELECT 1
      |FROM dummy
      |WHERE MAX(:input) > 1;
      """.trimMargin(),
      tempFolder,
    )

    val errorMessage = assertFailsWith<IllegalStateException> {
      file.findChildrenOfType<SqlBindExpr>().single().argumentType()
    }
    assertThat(errorMessage.message)
      .isEqualTo("The Kotlin type of the argument cannot be inferred, use CAST instead.")
  }

  @Test fun `bind arg kotlin type can be inferred with other types`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE dummy(
      |  foo INTEGER
      |);
      |
      |inferredNullableLong:
      |SELECT 1
      |FROM dummy
      |WHERE MAX(1, :input) > 1;
      """.trimMargin(),
      tempFolder,
    )

    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.let { args ->
      assertThat(args[0].dialectType).isEqualTo(PrimitiveType.INTEGER)
      assertThat(args[0].javaType).isEqualTo(Long::class.asClassName().copy(nullable = true))
    }
  }

  @Test fun `bind arg kotlin type cannot be inferred with other different types`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE dummy(
      |  foo INTEGER
      |);
      |
      |differentSqlTypes:
      |SELECT 1
      |FROM dummy
      |WHERE MAX(1, 'FOO', :input) > 1;
      """.trimMargin(),
      tempFolder,
    )

    val errorMessage = assertFailsWith<IllegalStateException> {
      file.findChildrenOfType<SqlBindExpr>().single().argumentType()
    }
    assertThat(errorMessage.message)
      .isEqualTo("The Kotlin type of the argument cannot be inferred, use CAST instead.")
  }

  @Test fun `bind args use proper binary operator precedence`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE User (
      |  type TEXT,
      |  first_name TEXT,
      |  last_name TEXT
      |);
      |
      |someSelect:
      |SELECT *
      |  FROM User
      | WHERE type = ?
      |   AND first_name LIKE ?
      |   AND last_name LIKE ?;
      """.trimMargin(),
      tempFolder,
    )

    val columns = file.findChildrenOfType<SqlColumnDef>().toTypedArray()
    file.findChildrenOfType<SqlBindExpr>().map { it.argumentType() }.let { args ->
      assertThat(args[0].dialectType).isEqualTo(PrimitiveType.TEXT)
      assertThat(args[0].javaType).isEqualTo(String::class.asClassName().copy(nullable = true))
      assertThat(args[0].name).isEqualTo("type")
      assertThat(args[0].column).isEqualTo(columns[0])

      assertThat(args[1].dialectType).isEqualTo(PrimitiveType.TEXT)
      assertThat(args[1].javaType).isEqualTo(String::class.asClassName())
      assertThat(args[1].name).isEqualTo("first_name")

      assertThat(args[2].dialectType).isEqualTo(PrimitiveType.TEXT)
      assertThat(args[2].javaType).isEqualTo(String::class.asClassName())
      assertThat(args[2].name).isEqualTo("last_name")
    }
  }

  @Test fun `bind arg type in binary expression can be inferred from column`() {
    val file = FixtureCompiler.parseSql(
      """
        |CREATE TABLE data (
        |  datum INTEGER NOT NULL
        |);
        |
        |selectData:
        |SELECT *
        |FROM data
        |WHERE datum > :datum1 - 2.5 AND datum < :datum2 + 2.5;
      """.trimMargin(),
      tempFolder,
    )
    val column = file.namedQueries.first()
    column.parameters.let { args ->
      assertThat(args[0].dialectType).isEqualTo(PrimitiveType.INTEGER)
      assertThat(args[0].javaType).isEqualTo(Long::class.asClassName())
      assertThat(args[0].name).isEqualTo("datum1")

      assertThat(args[1].dialectType).isEqualTo(PrimitiveType.INTEGER)
      assertThat(args[1].javaType).isEqualTo(Long::class.asClassName())
      assertThat(args[1].name).isEqualTo("datum2")
    }
  }

  @Test fun `bind arg in arithmetic binary expression can be cast as type`() {
    val file = FixtureCompiler.parseSql(
      """
        |CREATE TABLE data (
        | datum INTEGER NOT NULL,
        | point INTEGER NOT NULL
        |);
        |
        |selectData:
        |SELECT *, (datum + CAST(:datum1 AS REAL) * point) AS expected_datum
        |FROM data;
      """.trimMargin(),
      tempFolder,
    )

    val column = file.namedQueries.first()
    column.parameters.let { args ->
      assertThat(args[0].dialectType).isEqualTo(PrimitiveType.REAL)
      assertThat(args[0].javaType).isEqualTo(Double::class.asClassName().copy(nullable = true))
      assertThat(args[0].name).isEqualTo("datum1")
    }
  }

  @Test fun `bind arg in binary expression can be cast as type`() {
    val file = FixtureCompiler.parseSql(
      """
        |CREATE TABLE data (
        |  datum INTEGER NOT NULL
        |);
        |
        |selectData:
        |SELECT CAST(:datum1 AS REAL) + CAST(:datum2 AS INTEGER) - 10.5
        |FROM data;
      """.trimMargin(),
      tempFolder,
    )

    val column = file.namedQueries.first()
    column.parameters.let { args ->
      assertThat(args[0].dialectType).isEqualTo(PrimitiveType.REAL)
      assertThat(args[0].javaType).isEqualTo(Double::class.asClassName().copy(nullable = true))
      assertThat(args[0].name).isEqualTo("datum1")

      assertThat(args[1].dialectType).isEqualTo(PrimitiveType.INTEGER)
      assertThat(args[1].javaType).isEqualTo(Long::class.asClassName().copy(nullable = true))
      assertThat(args[1].name).isEqualTo("datum2")
    }
  }

  @Test fun `bind arg in binary expression can be cast as custom type`() {
    val file = FixtureCompiler.parseSql(
      """
        |import java.time.Instant;
        |
        |CREATE TABLE session (
        |  id UUID PRIMARY KEY,
        |  created_at TIMESTAMP AS Instant NOT NULL,
        |  updated_at TIMESTAMP AS Instant NOT NULL
        |);
        |
        |selectSession1:
        |SELECT *
        |FROM session
        |WHERE created_at = :createdAt - INTERVAL '2 days' OR updated_at = :updatedAt + INTERVAL '2 days';
        |
        |selectSession2:
        |SELECT *
        |FROM session
        |WHERE created_at BETWEEN :createdAt - INTERVAL '2 days' AND :createdAt + INTERVAL '2 days';
      """.trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect(),
    )

    val selectSession1 = file.namedQueries[0]
    selectSession1.parameters.let { args ->
      assertThat(args[0].javaType).isEqualTo(Instant::class.asClassName())
      assertThat(args[0].name).isEqualTo("createdAt")

      assertThat(args[1].javaType).isEqualTo(Instant::class.asClassName())
      assertThat(args[1].name).isEqualTo("updatedAt")
    }

    val selectSession2 = file.namedQueries[1]
    selectSession2.parameters.let { args ->
      assertThat(args[0].javaType).isEqualTo(Instant::class.asClassName())
      assertThat(args[0].name).isEqualTo("createdAt")
    }
  }

  private fun SqlBindExpr.argumentType() = typeResolver.argumentType(this)
}
