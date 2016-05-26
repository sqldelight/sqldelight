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
package com.squareup.sqldelight.reference

import com.squareup.sqldelight.SqlDelightFixtureTestCase

class ReferenceTests : SqlDelightFixtureTestCase() {
  override val fixtureDirectory = "reference"

  fun testTableName() {
    doTestReference("Test1.sq", "caret1")
  }

  fun testColumnName() {
    doTestReference("Test1.sq", "caret2")
  }

  fun testViewName() {
    doTestReference("Test1.sq", "caret3")
  }

  fun testColumnAlias() {
    doTestReference("Test1.sq", "caret5")
  }

  fun testColumnThroughView() {
    doTestReference("Test1.sq", "caret2")
  }

  fun testCommonTable() {
    doTestReference("Test1.sq", "caret4")
  }

  fun testUnion() {
    doTestReference("Test1.sq", "caret1")
  }

  fun testQuotedIdentifier() {
    doTestReference("Test1.sq", "caret13")
  }
}