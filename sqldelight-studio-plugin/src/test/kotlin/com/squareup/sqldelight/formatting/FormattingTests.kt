package com.squareup.sqldelight.formatting

import com.google.common.truth.Truth.assertThat
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import java.io.File

class FormattingTests : LightPlatformCodeInsightFixtureTestCase() {
  override fun getTestDataPath() = "testData/formatting"

  fun testFormatting() {
    myFixture.configureByFile("Formatting_before.sq")
    myFixture.performEditorAction("ReformatCode")
    assertThat(myFixture.file.text).isEqualTo(File("$testDataPath/Formatting_after.sq").readText())
  }

  fun testCreateTableAs() {
    myFixture.configureByFile("Formatting_create_table_before.sq")
    myFixture.performEditorAction("ReformatCode")
    assertThat(myFixture.file.text).isEqualTo(File("$testDataPath/Formatting_create_table_after.sq").readText())
  }
}
