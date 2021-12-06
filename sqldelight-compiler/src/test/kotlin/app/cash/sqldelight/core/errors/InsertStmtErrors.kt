package app.cash.sqldelight.core.errors

import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InsertStmtErrors {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `single column without default values not provided`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  value1 TEXT NOT NULL,
      |  value2 TEXT NOT NULL,
      |  value3 TEXT NOT NULL DEFAULT 'test',
      |  value4 TEXT NOT NULL
      |);
      |
      |insert:
      |INSERT INTO test (value1, value2)
      |VALUES ?;
      |""".trimMargin(),
      tempFolder
    )

    assertThat(result.errors).hasSize(1)
    assertThat(result.errors).contains("Test.sq: (9, 0): Cannot populate default value for column value4, it must be specified in insert statement.")
  }

  @Test fun `multiple columns without default values not provided`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  value1 TEXT NOT NULL,
      |  value2 TEXT NOT NULL,
      |  value3 TEXT NOT NULL,
      |  value4 TEXT NOT NULL
      |);
      |
      |insert:
      |INSERT INTO test (value1, value2)
      |VALUES ?;
      |""".trimMargin(),
      tempFolder
    )

    assertThat(result.errors).hasSize(1)
    assertThat(result.errors).contains("Test.sq: (9, 0): Cannot populate default values for columns (value3, value4), they must be specified in insert statement.")
  }
}
