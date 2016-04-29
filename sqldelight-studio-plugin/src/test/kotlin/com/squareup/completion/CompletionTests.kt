/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.completion

import com.google.common.truth.Truth.assertThat
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.squareup.sqldelight.intellij.lang.SqlDelightFileViewProvider
import com.squareup.sqldelight.types.SymbolTable

class CompletionTests : LightPlatformCodeInsightFixtureTestCase() {
  override fun getTestDataPath() = "testData/completion"

  val filesToLoad = arrayOf(
      "main/sqldelight/com/sample/Test1.sq",
      "main/sqldelight/com/sample/Test2.sq",
      "main/sqldelight/com/sample/Test3.sq"
  )

  override fun setUp() {
    super.setUp()
    SqlDelightFileViewProvider.symbolTable = SymbolTable()
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
    myFixture.configureByFile("main/sqldelight/com/sample/${getTestName(false)}.sq")
    myFixture.complete(CompletionType.BASIC, 1);
    assertThat(myFixture.lookupElementStrings).containsExactly(*variants)
  }
}