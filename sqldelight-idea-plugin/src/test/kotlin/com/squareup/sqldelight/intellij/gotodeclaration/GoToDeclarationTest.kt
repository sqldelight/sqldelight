package com.squareup.sqldelight.intellij.gotodeclaration

import com.alecstrong.sqlite.psi.core.psi.SqliteColumnName
import com.alecstrong.sqlite.psi.core.psi.SqliteForeignTable
import com.alecstrong.sqlite.psi.core.psi.SqliteTableName
import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase

class GoToDeclarationTest : SqlDelightFixtureTestCase() {
  fun testForeignKeyTableResolves() {
    myFixture.configureByText("ForeignTable.sq", """
      |CREATE TABLE foreignTable (
      |  value INTEGER NOT NULL PRIMARY KEY
      |);
      """.trimMargin())

    val foreignTable = file.findChildrenOfType<SqliteTableName>().single()

    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE localTable (
      |  value INTEGER NOT NULL PRIMARY KEY,
      |  foreign_value INTEGER NOT NULL REFERENCES foreignTable(value)
      |);
      """.trimMargin())
    val element = file.findChildrenOfType<SqliteForeignTable>().single()

    assertThat(element.reference!!.resolve()).isEqualTo(foreignTable)
  }

  fun testForeignKeyColumnResolves() {
    myFixture.configureByText("ForeignTable.sq", """
      |CREATE TABLE foreignTable (
      |  value INTEGER NOT NULL PRIMARY KEY
      |);
      """.trimMargin())

    val foreignColumn = file.findChildrenOfType<SqliteColumnName>().single()

    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE localTable (
      |  foreign_value INTEGER NOT NULL REFERENCES foreignTable(value)
      |);
      """.trimMargin())
    val element = file.findChildrenOfType<SqliteColumnName>().single { it.name == "value" }

    assertThat(element.reference!!.resolve()).isEqualTo(foreignColumn)
  }
}