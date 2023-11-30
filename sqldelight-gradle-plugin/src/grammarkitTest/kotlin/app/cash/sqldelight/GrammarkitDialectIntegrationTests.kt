package app.cash.sqldelight

import com.google.common.truth.Truth
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

class GrammarkitDialectIntegrationTests {
  @Test
  fun customFunctionDialect() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/custom-dialect"))
      .withArguments("clean", "compileTestKotlin", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `dialect accepts version catalog dependency`() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-catalog"))
      .withArguments("clean", "compileTestKotlin", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  private fun GradleRunner.withCommonConfiguration(projectRoot: File): GradleRunner {
    File(projectRoot, "gradle.properties").writeText(
      """
      |org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
      |
      """.trimMargin(),
    )
    return withProjectDir(projectRoot)
  }
}
