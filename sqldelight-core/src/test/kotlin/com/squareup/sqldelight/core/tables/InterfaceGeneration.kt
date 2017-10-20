package com.squareup.sqldelight.core.tables

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.core.util.FixtureCompiler
import org.junit.Test

class InterfaceGeneration {

  @Test fun requiresAdapter() {
    checkFixtureCompiles("requires-adapter")
  }

  private fun checkFixtureCompiles(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture(
        "src/test/table-interface-fixtures/$fixtureRoot",
        SqlDelightCompiler::writeTableInterfaces)
    for ((expectedFile, actualOutput) in result.compilerOutput) {
      assertThat(expectedFile.exists()).named("No file with name $expectedFile").isTrue()
      assertThat(expectedFile.readText()).named(expectedFile.name).isEqualTo(
          actualOutput.toString())
    }
  }
}
