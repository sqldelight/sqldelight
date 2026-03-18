package app.cash.sqldelight.tests

import app.cash.sqldelight.withCommonConfiguration
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

class SourcesTest {
  @Test fun `srcDirs generates correctly`() {
    val fixtureRoot = File("src/test/sources")
    val schemaFile = File(fixtureRoot, "main/sqldelight/databases/1.db")
    if (schemaFile.exists()) schemaFile.delete()

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateMainDatabaseSchema", "--stacktrace")
      .build()

    // verify
    assertThat(schemaFile.exists())
      .isTrue()

    schemaFile.delete()
  }
}
