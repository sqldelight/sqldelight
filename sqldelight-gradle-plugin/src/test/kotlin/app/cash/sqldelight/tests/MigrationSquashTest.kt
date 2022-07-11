package app.cash.sqldelight.tests

import app.cash.sqldelight.withCommonConfiguration
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(TestParameterInjector::class)
class MigrationSquashTest {
  @Test fun `squash migrations`(
    @TestParameter("mysql", "postgres", "sqlite") dialect: String,
  ) {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-squash"))
      .withArguments("clean", "squashMain${dialect}DatabaseMigrations", "--stacktrace")
      .withDebug(true)
      .build()

    assertThat(output.output).contains("BUILD SUCCESSFUL")

    File("src/test/migration-squash/expected/$dialect").listFiles()!!.forEach { expected ->
      val actual = File("src/test/migration-squash/src/main/sqldelight/$dialect/migrations/${expected.name}")
      assertWithMessage("Expected ${expected.name} to be generated").that(actual.exists()).isTrue()
      try {
        assertWithMessage(
          "The squashed migration ${expected.name} looks different when generated fresh:",
        ).that(actual.readText()).isEqualTo(expected.readText())
      } finally {
        actual.delete()
      }
    }
  }
}
