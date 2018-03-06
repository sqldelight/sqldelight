package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Test

class InterfaceGeneration {

  @Test fun noUniqueQueries() {
    checkFixtureCompiles("no-unique-queries")
  }

  @Test fun queryRequiresType() {
    checkFixtureCompiles("query-requires-type")
  }

  private fun checkFixtureCompiles(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture(
        "src/test/query-interface-fixtures/$fixtureRoot",
        SqlDelightCompiler::writeQueryInterfaces,
        false)
    for ((expectedFile, actualOutput) in result.compilerOutput) {
      assertThat(expectedFile.exists()).named("No file with name $expectedFile").isTrue()
      assertThat(expectedFile.readText()).named(expectedFile.name).isEqualTo(
          actualOutput.toString())
    }
  }
}
