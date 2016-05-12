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
package com.squareup.sqldelight.findusages

import com.squareup.sqldelight.SqlDelightFixtureTestCase
import com.squareup.sqldelight.assertThat

class FindUsagesTests : SqlDelightFixtureTestCase() {
  override val fixtureDirectory = "findusages"

  fun testTableName() {
    myFixture.testFindUsages("$sqldelightDir/TableName.sq").assertThat()
        .hasElementAtCaret("testData/$fixtureDirectory/$sqldelightDir/TableName.sq", "caret")
        .hasElementAtCaret("testData/main/Test1.sq", "caret1")
        .hasElementAtCaret("testData/main/Test1.sq", "caret6")
        .hasElementAtCaret("testData/main/Test1.sq", "caret7")
        .hasSize(4)
  }

  fun testColumnName() {
    myFixture.testFindUsages("$sqldelightDir/ColumnName.sq").assertThat()
        .hasElementAtCaret("testData/$fixtureDirectory/$sqldelightDir/ColumnName.sq", "caret")
        .hasElementAtCaret("testData/main/Test1.sq", "caret2")
        .hasElementAtCaret("testData/main/Test1.sq", "caret8")
        .hasElementAtCaret("testData/main/Test1.sq", "caret9")
        .hasElementAtCaret("testData/main/Test1.sq", "caret10")
        .hasSize(5)
  }

  fun testViewName() {
    myFixture.testFindUsages("$sqldelightDir/ViewName.sq").assertThat()
        .hasElementAtCaret("testData/$fixtureDirectory/$sqldelightDir/ViewName.sq", "caret")
        .hasElementAtCaret("testData/main/Test1.sq", "caret11")
        .hasSize(2)
  }

  fun testAliasedColumn() {
    myFixture.testFindUsages("$sqldelightDir/AliasedColumn.sq").assertThat()
        .hasElementAtCaret("testData/$fixtureDirectory/$sqldelightDir/AliasedColumn.sq", "caret")
        .hasElementAtCaret("testData/main/Test1.sq", "caret4")
        .hasElementAtCaret("testData/main/Test1.sq", "caret12")
        .hasSize(3)
  }

  fun testCommonTable() {
    myFixture.testFindUsages("$sqldelightDir/CommonTable.sq").assertThat()
        .hasElementAtCaret("testData/$fixtureDirectory/$sqldelightDir/CommonTable.sq", "caret")
        // Theres also a usage on line 2 in CommonTable.sq - TODO build something to check that.
        .hasSize(2)
  }
}
