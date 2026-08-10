package app.cash.sqldelight.tests

import app.cash.sqldelight.withCommonConfiguration
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

class CodegenExcludedColumnsGradleTest {
  @Test fun `codegenExcludedColumns omits columns from generated models`() {
    val fixtureRoot = File("src/test/codegen-excluded-columns")

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseInterface", "--stacktrace")
      .build()

    val generatedInterface = File(
      fixtureRoot,
      "build/generated/sqldelight/code/Database/main/com/sample/Test.kt",
    ).readText()
    assertThat(generatedInterface).contains("value_: String")
    assertThat(generatedInterface).doesNotContain("removed")

    val generatedQueries = File(
      fixtureRoot,
      "build/generated/sqldelight/code/Database/main/com/sample/TestQueries.kt",
    ).readText()
    assertThat(generatedQueries).contains("INSERT INTO test (id, value)")
    assertThat(generatedQueries).contains("SELECT test.id, test.value")
    assertThat(generatedQueries).doesNotContain("removed")
  }
}
