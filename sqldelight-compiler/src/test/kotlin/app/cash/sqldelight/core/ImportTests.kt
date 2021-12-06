package app.cash.sqldelight.core

import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImportTests {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `conflicting imports fails`() {
    val result = FixtureCompiler.compileSql(
      """
      |import com.fake.Thing;
      |import com.fake2.Thing;
      |
      |CREATE TABLE test (
      |  _id INTEGER NOT NULL PRIMARY KEY
      |);
      """.trimMargin(),
      tempFolder
    )

    assertThat(result.errors).containsExactly(
      "Test.sq: (1, 0): Multiple imports for type Thing",
      "Test.sq: (2, 0): Multiple imports for type Thing"
    )
  }
}
