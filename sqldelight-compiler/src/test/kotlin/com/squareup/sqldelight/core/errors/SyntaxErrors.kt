package com.squareup.sqldelight.core.errors

import com.google.common.truth.Truth
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SyntaxErrors {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `incorrect capitalization on type errors gracefully`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE TABLE my_table (
      |    col INTEGER AS long
      |);
      |""".trimMargin(), tempFolder)

    Truth.assertThat(result.errors)
        .hasSize(1)
    Truth.assertThat(result.errors)
        .contains("Test.sq line 2:19 - Unknown type long")
  }
}