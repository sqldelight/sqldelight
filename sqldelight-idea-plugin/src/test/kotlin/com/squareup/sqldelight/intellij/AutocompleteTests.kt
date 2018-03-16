package com.squareup.sqldelight.intellij

import com.google.common.truth.Truth.assertThat
import com.intellij.codeInsight.completion.CompletionType.BASIC
import com.squareup.sqldelight.core.lang.SqlDelightFileType

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
      |INSERT INTO test (<caret>)
    """.trimMargin())

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