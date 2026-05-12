package app.cash.sqldelight.core

import app.cash.sqldelight.dialects.mysql.MySqlDialect
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.fixtureRoot
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CodegenExcludedColumnsTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `codegen excluded columns are omitted from generated models`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test(
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT NOT NULL,
      |  removed TEXT
      |);
      |
      |insertTest:
      |INSERT INTO test
      |VALUES ?;
      |
      |selectAll:
      |SELECT *
      |FROM test;
      """.trimMargin(),
      tempFolder,
      codegenExcludedColumns = setOf("test.removed"),
    )

    assertThat(result.errors).isEmpty()

    val generatedInterface = result.compilerOutput[File(result.outputDirectory, "com/example/Test.kt")]
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).contains("value_: String")
    assertThat(generatedInterface.toString()).doesNotContain("removed")

    val generatedQueries = result.compilerOutput[File(result.outputDirectory, "com/example/TestQueries.kt")]
    assertThat(generatedQueries).isNotNull()
    assertThat(generatedQueries.toString()).contains("INSERT INTO test (id, value)")
    assertThat(generatedQueries.toString()).contains("SELECT test.id, test.value")
    assertThat(generatedQueries.toString()).doesNotContain("removed")
  }

  @Test fun `codegen excluded columns are omitted with mysql dialect`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test(
      |  id BIGINT NOT NULL PRIMARY KEY,
      |  value VARCHAR(191) NOT NULL,
      |  removed VARCHAR(191)
      |);
      |
      |insertTest:
      |INSERT INTO test
      |VALUES ?;
      |
      |selectAll:
      |SELECT *
      |FROM test;
      """.trimMargin(),
      tempFolder,
      overrideDialect = MySqlDialect(),
      codegenExcludedColumns = setOf("test.removed"),
    )

    assertThat(result.errors).isEmpty()

    val generatedInterface = result.compilerOutput[File(result.outputDirectory, "com/example/Test.kt")]
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).contains("value_: String")
    assertThat(generatedInterface.toString()).doesNotContain("removed")

    val generatedQueries = result.compilerOutput[File(result.outputDirectory, "com/example/TestQueries.kt")]
    assertThat(generatedQueries).isNotNull()
    assertThat(generatedQueries.toString()).contains("INSERT INTO test (id, value)")
    assertThat(generatedQueries.toString()).contains("SELECT test.id, test.value")
    assertThat(generatedQueries.toString()).doesNotContain("removed")
  }

  @Test fun `codegen excluded columns are omitted with postgresql dialect`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test(
      |  id BIGINT NOT NULL PRIMARY KEY,
      |  value TEXT NOT NULL,
      |  removed TEXT
      |);
      |
      |insertTest:
      |INSERT INTO test
      |VALUES ?;
      |
      |selectAll:
      |SELECT *
      |FROM test;
      """.trimMargin(),
      tempFolder,
      overrideDialect = PostgreSqlDialect(),
      codegenExcludedColumns = setOf("test.removed"),
    )

    assertThat(result.errors).isEmpty()

    val generatedInterface = result.compilerOutput[File(result.outputDirectory, "com/example/Test.kt")]
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).contains("value_: String")
    assertThat(generatedInterface.toString()).doesNotContain("removed")

    val generatedQueries = result.compilerOutput[File(result.outputDirectory, "com/example/TestQueries.kt")]
    assertThat(generatedQueries).isNotNull()
    assertThat(generatedQueries.toString()).contains("INSERT INTO test (id, value)")
    assertThat(generatedQueries.toString()).contains("SELECT test.id, test.value")
    assertThat(generatedQueries.toString()).doesNotContain("removed")
  }

  @Test fun `codegen excluded columns added by migrations are omitted from generated models`() {
    FixtureCompiler.writeSql(
      """
      |CREATE TABLE test(
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT NOT NULL
      |);
      """.trimMargin(),
      tempFolder,
      "0.sqm",
    )
    FixtureCompiler.writeSql(
      """
      |ALTER TABLE test ADD COLUMN removed TEXT;
      """.trimMargin(),
      tempFolder,
      "1.sqm",
    )
    FixtureCompiler.writeSql(
      """
      |insertTest:
      |INSERT INTO test
      |VALUES ?;
      |
      |selectAll:
      |SELECT *
      |FROM test;
      """.trimMargin(),
      tempFolder,
      "Test.sq",
    )

    val result = FixtureCompiler.compileFixture(
      tempFolder.fixtureRoot().path,
      deriveSchemaFromMigrations = true,
      codegenExcludedColumns = setOf("test.removed"),
    )

    assertThat(result.errors).isEmpty()

    val generatedInterface = result.compilerOutput[File(result.outputDirectory, "com/example/Test.kt")]
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).contains("value_: String")
    assertThat(generatedInterface.toString()).doesNotContain("removed")

    val generatedQueries = result.compilerOutput[File(result.outputDirectory, "com/example/TestQueries.kt")]
    assertThat(generatedQueries).isNotNull()
    assertThat(generatedQueries.toString()).contains("INSERT INTO test (id, value)")
    assertThat(generatedQueries.toString()).contains("SELECT test.id, test.value")
    assertThat(generatedQueries.toString()).doesNotContain("removed")
  }

  @Test fun `unknown codegen excluded table fails compilation`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test(
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT NOT NULL
      |);
      """.trimMargin(),
      tempFolder,
      codegenExcludedColumns = setOf("missing.removed"),
    )

    assertThat(result.errors).containsExactly(
      "Unknown table 'missing' referenced by codegenExcludedColumns value 'missing.removed'.",
    )
  }

  @Test fun `unknown codegen excluded column fails compilation`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test(
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT NOT NULL
      |);
      """.trimMargin(),
      tempFolder,
      codegenExcludedColumns = setOf("test.removed"),
    )

    assertThat(result.errors).containsExactly(
      "Unknown column 'removed' on table 'test' referenced by codegenExcludedColumns value 'test.removed'.",
    )
  }

  @Test fun `explicit insert column list with codegen excluded column fails compilation`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test(
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT NOT NULL,
      |  removed TEXT
      |);
      |
      |insertTest:
      |INSERT INTO test (id, value, removed)
      |VALUES ?;
      """.trimMargin(),
      tempFolder,
      codegenExcludedColumns = setOf("test.removed"),
    )

    assertThat(result.errors).containsExactly(
      "Column 'removed' on table 'test' is excluded from codegen but is explicitly listed in an INSERT statement. " +
        "Remove it from the query or from codegenExcludedColumns value 'test.removed'.",
    )
  }

  @Test fun `malformed codegen excluded column fails compilation`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test(
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT NOT NULL
      |);
      """.trimMargin(),
      tempFolder,
      codegenExcludedColumns = setOf("test"),
    )

    assertThat(result.errors).containsExactly(
      "Invalid codegenExcludedColumns value 'test'. Expected format: table.column",
    )
  }

  @Test fun `computed query aliases are not matched as codegen excluded columns`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test(
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT NOT NULL,
      |  removed TEXT
      |);
      |
      |selectComputed:
      |SELECT value || 'suffix' AS computed_value
      |FROM test;
      """.trimMargin(),
      tempFolder,
      codegenExcludedColumns = setOf("test.removed"),
    )

    assertThat(result.errors).isEmpty()

    val generatedQueries = result.compilerOutput[File(result.outputDirectory, "com/example/TestQueries.kt")]
    assertThat(generatedQueries).isNotNull()
    assertThat(generatedQueries.toString()).contains("computed_value")
  }

  @Test fun `virtual table query columns are not matched as codegen excluded columns`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test(
      |  removed TEXT
      |);
      |
      |CREATE VIRTUAL TABLE email USING fts5(
      |  sender,
      |  title,
      |  body UNINDEXED
      |);
      |
      |someSelect:
      |SELECT sender, title, body
      |FROM email
      |WHERE email MATCH ? AND sender = ?;
      """.trimMargin(),
      tempFolder,
      codegenExcludedColumns = setOf("test.removed"),
    )

    assertThat(result.errors).isEmpty()

    val generatedInterface = result.compilerOutput[File(result.outputDirectory, "com/example/SomeSelect.kt")]
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).contains("public val sender: String?")
  }
}
