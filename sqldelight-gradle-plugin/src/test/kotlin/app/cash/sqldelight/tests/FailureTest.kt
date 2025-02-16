package app.cash.sqldelight.tests

import app.cash.sqldelight.withCommonConfiguration
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

class FailureTest {
  @Test fun `missing package directory fails properly`() {
    val fixtureRoot = File("src/test/no-package")

    val output = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseInterface", "--stacktrace")
      .buildAndFail()

    assertThat(output.output).contains("Compiling with dialect app.cash.sqldelight.dialects.sqlite_3_18.SqliteDialect")
    assertThat(output.output).contains(
      """
      |NoPackage.sq:1:0 SqlDelight files must be placed in a package directory.
      |1    CREATE TABLE test (
      |2      value TEXT
      |3    );
      |
      """.trimMargin(),
    )
  }

  @Test fun `errors with tabs in line underline properly`() {
    val fixtureRoot = File("src/test/bad-underlines")

    val output = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseInterface", "--stacktrace")
      .buildAndFail()

    assertThat(output.output).contains(
      """
      |Test1.sq:2:7 <type name real> expected, got 'BIGINT'
      |1    CREATE TABLE test1(
      |2    	value	BIGINT
      |     	     	^^^^^^
      |3    )
      """.trimMargin(),
    )
  }
}
