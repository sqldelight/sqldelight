package app.cash.sqldelight.core

import app.cash.sqldelight.test.util.FixtureCompiler
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
