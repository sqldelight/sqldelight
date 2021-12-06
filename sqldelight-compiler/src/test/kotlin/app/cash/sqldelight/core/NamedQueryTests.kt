package app.cash.sqldelight.core

import app.cash.sqldelight.core.lang.util.TableNameElement.CreateTableName
import app.cash.sqldelight.core.lang.util.TableNameElement.NewTableName
import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NamedQueryTests {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `tablesObserved returns a list of all tables observed`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val table = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()

    assertThat(query.tablesObserved).containsExactly(CreateTableName(table.tableName))
  }

  @Test fun `tablesObserved resolves table aliases properly`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT data2.*
      |FROM data AS data2
      |WHERE data2.id = ?;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val table = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()

    assertThat(query.tablesObserved).containsExactly(CreateTableName(table.tableName))
  }

  @Test fun `tablesObserved resolves views properly`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |CREATE VIEW some_view AS
      |SELECT *
      |FROM data;
      |
      |selectForId:
      |SELECT *
      |FROM some_view;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val table = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()

    assertThat(query.tablesObserved).containsExactly(CreateTableName(table.tableName))
  }

  @Test fun `tablesObserved resolves common tables properly`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |WITH common_table AS (
      |  SELECT *
      |  FROM data
      |)
      |SELECT *
      |FROM common_table;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val table = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()

    assertThat(query.tablesObserved).containsExactly(CreateTableName(table.tableName))
  }

  @Test fun `tablesObserved resolves recursive common tables properly`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |WITH RECURSIVE
      |  cnt(x) AS (SELECT id FROM data UNION ALL SELECT x+1 FROM cnt WHERE x<1000000)
      |SELECT x FROM cnt;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val table = file.sqliteStatements().mapNotNull { it.statement.createTableStmt }.first()

    assertThat(query.tablesObserved).containsExactly(CreateTableName(table.tableName))
  }

  @Test fun `tablesObserved correctly returns migration name`() {
    val result = FixtureCompiler.compileFixture(
      fixtureRoot = "src/test/migration-interface-fixtures/alter-table",
      generateDb = false,
      deriveSchemaFromMigrations = true
    )

    val query = result.compiledFile.namedQueries.first()

    val tableObserved = query.tablesObserved.single()
    assertThat(tableObserved).isInstanceOf(NewTableName::class.java)
    assertThat(tableObserved.name).isEqualTo("new_test")
  }
}
