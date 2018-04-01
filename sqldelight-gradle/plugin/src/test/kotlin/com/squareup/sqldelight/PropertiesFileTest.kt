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
        .withArguments("clean", "generateSqlDelightInterface", "--stacktrace",
            "-Dsqldelight.skip.runtime=true")
        .build()

    // verify
    val propertiesFile = File(fixtureRoot, ".idea/sqldelight/${SqlDelightPropertiesFile.NAME}")
    assertThat(propertiesFile.exists()).isTrue()

    val properties = SqlDelightPropertiesFile.fromFile(propertiesFile)
    assertThat(properties.packageName).isEqualTo("com.example")
    assertThat(properties.outputDirectory).isEqualTo("build/sqldelight")
    assertThat(properties.sourceSets).hasSize(1)

    with(properties.sourceSets[0]) {
      assertThat(this).hasSize(1)
      assertThat(this[0]).isEqualTo("src/main/sqldelight")
    }

    propertiesFile.delete()
  }
}