package app.cash.sqldelight.dialect

import com.google.common.truth.Truth
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

class DialectIntegrationTests {

  @Test fun integrationTestsMySql() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-mysql"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsMySqlAsync() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-mysql-async"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsMySqlSchemaDefinitions() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-mysql-schema"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsPostgreSql() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-postgresql"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsPostgreSqlAsync() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-postgresql-async"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun integrationTestsPostgreSqlMigrations() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/integration-postgresql-migrations"))
      .withArguments("clean", "check", "--stacktrace")

    val result = runner.build()
    Truth.assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
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
