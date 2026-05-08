package app.cash.sqldelight.tests

import app.cash.sqldelight.withCommonConfiguration
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

class TaskOptionsTest {
  @Test
  fun `generate task can exclude columns from generated code`() {
    val fixtureRoot = File("src/test/exclude-column")

    val result = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseInterface", "--exclude-column=test.removed", "--stacktrace")
      .build()

    assertThat(result.output).contains("BUILD SUCCESSFUL")

    val generatedInterface = File(fixtureRoot, "build/generated/sqldelight/code/Database/main/com/sample/Test.kt").readText()
    assertThat(generatedInterface).doesNotContain("removed")

    val generatedQueries = File(fixtureRoot, "build/generated/sqldelight/code/Database/main/com/sample/TestQueries.kt").readText()
    assertThat(generatedQueries).contains("INSERT INTO test (id, value)")
    assertThat(generatedQueries).contains("SELECT test.id, test.value")
    assertThat(generatedQueries).doesNotContain("removed")
  }

  @Test
  fun `generate task fails for unknown excluded columns`() {
    val fixtureRoot = File("src/test/exclude-column")

    val result = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseInterface", "--exclude-column=test.missing", "--stacktrace")
      .buildAndFail()

    assertThat(result.output)
      .contains("Unknown column 'missing' on table 'test' referenced by codegenExcludedColumns value 'test.missing'.")
  }
}
