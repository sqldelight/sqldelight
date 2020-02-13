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

package com.squareup.sqldelight.intellij.autocomplete

import com.google.common.truth.Truth.assertThat
import com.intellij.codeInsight.completion.CompletionType.BASIC
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase

class AutocompleteTests : SqlDelightFixtureTestCase() {
  fun testAutocompleteWorksOnUpdateTableNames() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  value TEXT
      |);
      |
      |CREATE TABLE test2 (
      |  value TEXT
      |);
      |
      |someUpdate:
      |UPDATE <caret>
    """.trimMargin())

    myFixture.complete(BASIC, 1).let {
      assertThat(it).hasLength(2)
      assertThat(it[0].lookupString).isEqualTo("test")
      assertThat(it[1].lookupString).isEqualTo("test2")
    }
  }

  fun testAutocompleteWorksOnUpdateColumnNames() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  value TEXT
      |);
      |
      |CREATE TABLE test2 (
      |  value2 TEXT
      |);
      |
      |someUpdate:
      |UPDATE test
      |SET <caret>
    """.trimMargin())

    myFixture.complete(BASIC, 1).let {
      assertThat(it).hasLength(1)
      assertThat(it[0].lookupString).isEqualTo("value")
    }
  }

  fun testAutocompleteWorksOnUpdateExpressions() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  value TEXT
      |);
      |
      |CREATE TABLE test2 (
      |  value2 TEXT
      |);
      |
      |someUpdate:
      |UPDATE test
      |SET value = <caret>
    """.trimMargin())

    myFixture.complete(BASIC, 1).let {
      assertThat(it).hasLength(2)
      assertThat(it[0].lookupString).isEqualTo("test")
      assertThat(it[1].lookupString).isEqualTo("value")
    }
  }

  fun testAutocompleteWorksOnInsertTableNames() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  value TEXT
      |);
      |
      |CREATE TABLE test2 (
      |  value2 TEXT
      |);
      |
      |someInsert:
      |INSERT INTO <caret>
    """.trimMargin())

    myFixture.complete(BASIC, 1).let {
      assertThat(it).hasLength(2)
      assertThat(it[0].lookupString).isEqualTo("test")
      assertThat(it[1].lookupString).isEqualTo("test2")
    }
  }

  fun testAutocompleteWorksOnInsertColumnNames() {
    val file = myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  value TEXT
      |);
      |
      |CREATE TABLE test2 (
      |  value2 TEXT
      |);
      |
      |someInsert:
      |INSERT INTO test (<caret>)
    """.trimMargin()) as SqlDelightFile

    fun PsiElement.printTree(printer: (String) -> Unit) {
      printer("$this\n")
      children.forEach { child ->
        child.printTree { printer("  $it") }
      }
    }

    file.printTree(::println)

    myFixture.complete(BASIC, 1).let {
      assertThat(it).hasLength(1)
      assertThat(it[0].lookupString).isEqualTo("value")
    }
  }

  fun testAutocompleteWorksOnDeleteTableName() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  value TEXT
      |);
      |
      |CREATE TABLE test2 (
      |  value2 TEXT
      |);
      |
      |someDelete:
      |DELETE FROM <caret>
    """.trimMargin())

    myFixture.complete(BASIC, 1).let {
      assertThat(it).hasLength(2)
      assertThat(it[0].lookupString).isEqualTo("test")
      assertThat(it[1].lookupString).isEqualTo("test2")
    }
  }

  fun testAutocompleteWorksOnSelectTableName() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  value TEXT
      |);
      |
      |CREATE TABLE test2 (
      |  value2 TEXT
      |);
      |
      |someSelect:
      |SELECT *
      |FROM <caret>
    """.trimMargin())

    myFixture.complete(BASIC, 1).let {
      assertThat(it).hasLength(2)
      assertThat(it[0].lookupString).isEqualTo("test")
      assertThat(it[1].lookupString).isEqualTo("test2")
    }
  }

  fun testAutocompleteWorksWhenFileHasErrors() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  value TEXT
      |);
      |
      |CREATE TABLE test2 (
      |  value2 TXT
      |);
      |
      |someSelect:
      |SELECT *
      |FROM <caret>
    """.trimMargin())

    myFixture.complete(BASIC, 1).let {
      assertThat(it).hasLength(2)
      assertThat(it[0].lookupString).isEqualTo("test")
      assertThat(it[1].lookupString).isEqualTo("test2")
    }
  }
}