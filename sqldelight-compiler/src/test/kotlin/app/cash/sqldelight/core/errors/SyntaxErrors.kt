package app.cash.sqldelight.core.errors

import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SyntaxErrors {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `incorrect capitalization on type errors gracefully`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE my_table (
      |    col INTEGER AS long
      |);
      |""".trimMargin(),
      tempFolder
    )

    assertThat(result.errors).containsExactly("Test.sq: (2, 19): Unknown type long")
  }

  @Test fun `unknown function`() {
    val result = FixtureCompiler.compileSql(
      """
      |selectScoob:
      |SELECT scoobyDoo();
      |""".trimMargin(),
      tempFolder
    )

    assertThat(result.errors).containsExactly("Test.sq: (2, 7): Unknown function scoobyDoo")
  }

  @Test fun `illegal type fails gracefully`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE my_table (
      |    col INTEGER AS team
      |);
      |""".trimMargin(),
      tempFolder
    )

    assertThat(result.errors).containsExactly("Test.sq: (2, 19): Unknown type team")
  }

  @Test fun `lowercase 'as' in column type`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE my_table (
      |    col INTEGER as Boolean
      |);
      |""".trimMargin(),
      tempFolder
    )

    assertThat(result.errors).containsExactly(
      "Test.sq: (2, 8): Reserved keyword in sqlite",
      "Test.sq: (2, 16): Expected 'AS', got 'as'"
    )
  }
}
