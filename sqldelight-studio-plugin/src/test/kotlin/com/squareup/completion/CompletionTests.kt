package com.squareup.completion

import com.google.common.truth.Truth.assertThat
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.squareup.sqldelight.intellij.lang.SqlDelightFileViewProvider

class CompletionTests : LightPlatformCodeInsightFixtureTestCase() {
  override fun getTestDataPath() = "testData/completion"

  val filesToLoad = arrayOf(
      "src/main/sqldelight/com/sample/Test1.sq",
      "src/main/sqldelight/com/sample/Test2.sq"
  )

  override fun setUp() {
    super.setUp()
    myFixture.configureByFiles(*filesToLoad)
    for (file in filesToLoad) {
      (myFixture.psiManager
          .findFile(myFixture.findFileInTempDir(file))!!.viewProvider as SqlDelightFileViewProvider)
          .generateJavaInterface()
    }
  }

  fun testTableName() {
    doTestVariants("different_view", "test1", "view1", "test2")
  }

  fun testResultColumn() {
    doTestVariants("column1", "test1")
  }

  fun testViewResultColumn() {
    doTestVariants("count", "different_column", "different_view")
  }

  fun testViewResultColumnPrefixed() {
    doTestVariants("count", "different_column")
  }

  fun testTableNamePrefixed() {
    doTestVariants("test1", "test2")
  }

  fun testExpression() {
    doTestVariants("test2", "different_column", "different_column2")
  }

  fun testAlias() {
    doTestVariants("testOne", "column1")
  }

  fun testJoinSubquery() {
    doTestVariants("test1", "column1", "test2", "different_column2")
  }

  fun testCommonTable() {
    doTestVariants("common_table", "common_column")
  }

  private fun doTestVariants(vararg variants: String) {
    myFixture.configureByFile("src/main/sqldelight/com/sample/${getTestName(false)}.sq")
    myFixture.complete(CompletionType.BASIC, 1);
    assertThat(myFixture.lookupElementStrings).containsExactly(*variants)
  }
}