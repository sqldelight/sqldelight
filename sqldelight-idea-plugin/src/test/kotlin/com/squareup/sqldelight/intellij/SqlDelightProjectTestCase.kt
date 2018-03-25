package com.squareup.sqldelight.intellij

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import java.io.File

abstract class SqlDelightProjectTestCase : LightCodeInsightFixtureTestCase() {
  override fun setUp() {
    super.setUp()
    configurePropertiesFile().toFile(File(testDataPath, SqlDelightPropertiesFile.NAME))
    myFixture.copyDirectoryToProject("", "")
  }

  override fun tearDown() {
    super.tearDown()
    File(testDataPath, SqlDelightPropertiesFile.NAME).delete()
  }

  override fun getTestDataPath() = "testData/project"

  open fun configurePropertiesFile(): SqlDelightPropertiesFile {
    return SqlDelightPropertiesFile(
        packageName = "com.example",
        sourceSets = listOf(
            listOf("src/main/sqldelight", "src/internal/sqldelight", "src/debug/sqldelight", "src/internalDebug/sqldelight"),
            listOf("src/main/sqldelight", "src/internal/sqldelight", "src/release/sqldelight", "src/internalRelease/sqldelight"),
            listOf("src/main/sqldelight", "src/production/sqldelight", "src/debug/sqldelight", "src/productionDebug/sqldelight"),
            listOf("src/main/sqldelight", "src/production/sqldelight", "src/release/sqldelight", "src/productionRelease/sqldelight")
        ),
        outputDirectory = "src/build/com/example"
    )
  }
}