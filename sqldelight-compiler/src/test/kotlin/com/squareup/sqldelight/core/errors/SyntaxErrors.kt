package com.squareup.sqldelight.core.errors

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.test.util.FixtureCompiler
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

    assertThat(result.errors).containsExactly("Test.sq line 2:19 - Unknown type long")
  }

  @Test fun `unknown function`() {
    val result = FixtureCompiler.compileSql(
      """
      |selectScoob:
      |SELECT scoobyDoo();
      |""".trimMargin(),
      tempFolder
    )

    assertThat(result.errors).containsExactly("Test.sq line 2:7 - Unknown function scoobyDoo")
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

    assertThat(result.errors).containsExactly("Test.sq line 2:19 - Unknown type team")
  }
}
