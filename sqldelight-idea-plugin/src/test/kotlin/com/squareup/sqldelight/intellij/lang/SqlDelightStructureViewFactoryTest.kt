/*
 * Copyright (C) 2018 Square, Inc.
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

package com.squareup.sqldelight.intellij.lang

import com.google.common.truth.Truth.assertThat
import com.intellij.navigation.NavigationItem
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase

class SqlDelightStructureViewFactoryTest : SqlDelightFixtureTestCase() {

  fun testStructureView() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE test (
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  title TEXT NOT NULL
      |);
      |
      |select_all:
      |SELECT *
      |FROM test;
      |
      |insert:
      |INSERT INTO test(title)
      |VALUES (?);
      |
      |CREATE VIEW select_all_view AS
      |SELECT *
      |FROM test;
      |
      |CREATE TRIGGER trigger_1
      |BEFORE INSERT ON test
      |BEGIN
      |  DELETE FROM test;
      |END;
      |
      |CREATE INDEX some_index
      |ON test (_id);
      |
      |CREATE VIRTUAL TABLE virtual_table USING custom_module;
      """.trimMargin()
    )

    myFixture.testStructureView { consumer ->
      with(consumer.treeModel.root) {
        val entries = children.map { (it as NavigationItem).name }
        assertThat(entries).isEqualTo(
          listOf(
            "CREATE TABLE test",
            "select_all",
            "insert",
            "CREATE VIEW select_all_view",
            "CREATE TRIGGER trigger_1",
            "CREATE INDEX some_index",
            "CREATE VIRTUAL TABLE virtual_table"
          )
        )
      }
    }
  }
}
