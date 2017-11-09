package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class VariantTest {
  @Test
  fun `A table queried from the main source set must be consistent for all variants`() {
    val fixtureRoot = File("src/test/fixtures/fulfilled-table-variant")
    val androidHome = androidHome()
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    var result = runner
        .withArguments("clean", "generateInternalSqlDelightInterface",
            "--stacktrace", "-Dsqldelight.skip.runtime=true")
        .buildAndFail()
    assertThat(result.output).contains("""
      MainTable.sq line 7:12 - No column found with name some_column1
      7    SELECT _id, some_column1
                       ^^^^^^^^^^^^
      8    FROM some_table

       FAILED
      """.trimIndent())

    runner.withArguments("clean", "generateReleaseSqlDelightInterface",
            "--stacktrace", "-Dsqldelight.skip.runtime=true")
        .build()
  }

  @Test
  fun `The gradle plugin resolves with multiple source sets`() {
    val fixtureRoot = File("src/test/fixtures/variants")
    val androidHome = androidHome()
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    val runner = GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()

    val result = runner
        .withArguments("clean", "assemble", "--stacktrace", "-Dsqldelight.skip.runtime=true",
            "--continue")
        .buildAndFail()
    assertThat(result.output).contains("""
      src/minApi21DemoDebug/sqldelight/com/sample/demo/debug/DemoDebug.sq line 7:5 - No table found with name full_table
      6    SELECT *
      7    FROM full_table
                ^^^^^^^^^^
      """.trimIndent())
  }
}
