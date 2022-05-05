package app.cash.sqldelight.tests

import app.cash.sqldelight.withCommonConfiguration
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class MigrationSquashTest {
  @Test fun `squash mysql migrations`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-squash"))
      .withArguments("clean", "squashMainMyDatabaseMigrations", "--stacktrace")
      .withDebug(true)
      .build()

    assertThat(output.output).contains("BUILD SUCCESSFUL")

    File("src/test/migration-squash/expected").listFiles()!!.forEach { expected ->
      val actual = File("src/test/migration-squash/src/main/sqldelight/migrations/${expected.name}")
      assertWithMessage("Expected ${expected.name} to be generated").that(actual.exists()).isTrue()
      try {
        assertWithMessage(
          "The squashed migration ${expected.name} looks different when generated fresh:"
        ).that(actual.readText()).isEqualTo(expected.readText())
      } finally {
        actual.delete()
      }
    }
  }
}
