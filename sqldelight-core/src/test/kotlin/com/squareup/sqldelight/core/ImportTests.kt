package com.squareup.sqldelight.core

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImportTests {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `conflicting imports fails`() {
    val result = FixtureCompiler.compileSql("""
      |import com.fake.Thing;
      |import com.fake2.Thing;
      |
      |CREATE TABLE test (
      |  _id INTEGER NOT NULL PRIMARY KEY
      |);
      """.trimMargin(), tempFolder)

    assertThat(result.errors).containsExactly(
        "Test.sq line 1:0 - Multiple imports for type Thing",
        "Test.sq line 2:0 - Multiple imports for type Thing"
    )
  }
}