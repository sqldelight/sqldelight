package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class PropertiesFileTest {
  @Test fun `properties file generates correctly`() {
    val fixtureRoot = File("src/test/properties-file")
    File(fixtureRoot, ".idea").mkdir()

    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
        .withArguments("clean", "generateMainDatabaseInterface", "--stacktrace")
        .build()

    // verify
    val propertiesFile = File(fixtureRoot, ".idea/sqldelight/${SqlDelightPropertiesFile.NAME}")
    assertThat(propertiesFile.exists()).isTrue()

    val properties = SqlDelightPropertiesFile.fromFile(propertiesFile).databases.single()
    assertThat(properties.packageName).isEqualTo("com.example")
    assertThat(properties.outputDirectory).isEqualTo("build/sqldelight/Database")
    assertThat(properties.compilationUnits).hasSize(1)

    with(properties.compilationUnits[0]) {
      assertThat(sourceFolders).containsExactly("src/main/sqldelight")
    }

    propertiesFile.delete()
  }
}
