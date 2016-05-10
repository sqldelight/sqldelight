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
package com.squareup.sqldelight.completion

import com.squareup.sqldelight.SqlDelightFixtureTestCase

class CompletionTests : SqlDelightFixtureTestCase() {
  override val fixtureDirectory = "completion"

  fun testTableName() {
    doTestVariants("different_view", "test1", "view1", "test2", "with_common_table")
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
}