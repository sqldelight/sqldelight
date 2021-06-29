package com.squareup.sqldelight.integrations

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.androidHome
import com.squareup.sqldelight.gradle.SqlDelightSourceFolderImpl
import com.squareup.sqldelight.properties
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class VariantTest {
  @Test
  fun `A table queried from the main source set must be consistent for all variants`() {
    val fixtureRoot = File("src/test/fulfilled-table-variant").absoluteFile
    val androidHome = androidHome()
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    val runner = GradleRunner.create()
      .withProjectDir(fixtureRoot)
      .setDebug(true) // Run in-process.

    val result = runner
      .withArguments("clean", "generateInternalDatabaseInterface", "--stacktrace")
      .buildAndFail()
    assertThat(result.output).contains(
      """
      MainTable.sq line 8:12 - No column found with name some_column1
      8    SELECT _id, some_column1
                       ^^^^^^^^^^^^
      9    FROM some_table
      """.trimIndent()
    )

    runner.withArguments(
      "clean", "generateReleaseDatabaseInterface",
      "--stacktrace", "-Dsqldelight.skip.runtime=true"
    )
      .build()
  }

  @Test
  fun `The gradle plugin resolves with multiple source sets`() {
    val fixtureRoot = File("src/test/variants").absoluteFile
    val androidHome = androidHome()
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    val runner = GradleRunner.create()
      .withProjectDir(fixtureRoot)
      .setDebug(true) // Run in-process.

    val result = runner
      .withArguments("clean", "assemble", "--stacktrace", "--continue")
      .buildAndFail()
    assertThat(result.output).contains(
      """
      src/minApi21DemoDebug/sqldelight/com/sample/demo/debug/DemoDebug.sq line 8:5 - No table found with name full_table
      7    SELECT *
      8    FROM full_table
                ^^^^^^^^^^
      """.trimIndent()
    )
  }

  @Test
  fun `The gradle plugin generates a properties file with the application id and all source sets`() {
    val fixtureRoot = File("src/test/working-variants").absoluteFile
    val androidHome = androidHome()
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    GradleRunner.create()
      .withProjectDir(fixtureRoot)
      .withArguments("clean", "--stacktrace", "--continue")
      .setDebug(true) // Run in-process.
      .build()

    // verify
    val properties = properties(fixtureRoot)!!.databases.single()
    assertThat(properties.packageName).isEqualTo("com.example.sqldelight")
    assertThat(properties.compilationUnits).hasSize(2)

    with(properties.compilationUnits[0]) {
      assertThat(sourceFolders).containsExactly(
        SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
        SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false)
      )
    }

    with(properties.compilationUnits[1]) {
      assertThat(sourceFolders).containsExactly(
        SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
        SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false)
      )
    }
  }
}
