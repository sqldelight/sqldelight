package app.cash.sqldelight.intellij.rename

import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase

class SqliteElementTests : SqlDelightFixtureTestCase() {
  fun testTableRename() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE <caret>test(
      |  value TEXT
      |);
      |
      |CREATE TABLE otherTest(
      |  value TEXT REFERENCES test(value)
      |);
      |
      |CREATE INDEX myIndex ON test(value);
      |
      |CREATE VIEW myView AS
      |SELECT *
      |FROM test;
      |
      |someInsert:
      |INSERT INTO test
      |VALUES (?);
      |
      |someSelect:
      |SELECT *
      |FROM test;
    """.trimMargin()
    )

    myFixture.renameElementAtCaret("newTest")
    myFixture.checkResult(
      """
      |CREATE TABLE newTest(
      |  value TEXT
      |);
      |
      |CREATE TABLE otherTest(
      |  value TEXT REFERENCES newTest(value)
      |);
      |
      |CREATE INDEX myIndex ON newTest(value);
      |
      |CREATE VIEW myView AS
      |SELECT *
      |FROM newTest;
      |
      |someInsert:
      |INSERT INTO newTest
      |VALUES (?);
      |
      |someSelect:
      |SELECT *
      |FROM newTest;
    """.trimMargin()
    )
  }

  fun testColumnRename() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE test(
      |  <caret>value TEXT
      |);
      |
      |CREATE TABLE otherTest(
      |  value TEXT REFERENCES test(value)
      |);
      |
      |CREATE INDEX myIndex ON test(value);
      |
      |CREATE VIEW myView AS
      |SELECT value
      |FROM test;
      |
      |someInsert:
      |INSERT INTO test (value)
      |VALUES (?);
      |
      |someSelect:
      |SELECT value
      |FROM test;
    """.trimMargin()
    )

    myFixture.renameElementAtCaret("newValue")
    myFixture.checkResult(
      """
      |CREATE TABLE test(
      |  newValue TEXT
      |);
      |
      |CREATE TABLE otherTest(
      |  value TEXT REFERENCES test(newValue)
      |);
      |
      |CREATE INDEX myIndex ON test(newValue);
      |
      |CREATE VIEW myView AS
      |SELECT newValue
      |FROM test;
      |
      |someInsert:
      |INSERT INTO test (newValue)
      |VALUES (?);
      |
      |someSelect:
      |SELECT newValue
      |FROM test;
    """.trimMargin()
    )
  }

  fun testViewRename() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE VIEW myView AS
      |SELECT 1;
      |
      |someSelect:
      |SELECT *
      |FROM <caret>myView;
    """.trimMargin()
    )

    myFixture.renameElementAtCaret("newView")
    myFixture.checkResult(
      """
      |CREATE VIEW newView AS
      |SELECT 1;
      |
      |someSelect:
      |SELECT *
      |FROM newView;
    """.trimMargin()
    )
  }

  fun testColumnAliasRename() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE VIEW myView AS
      |SELECT 1 AS columnAlias;
      |
      |someSelect:
      |SELECT <caret>columnAlias
      |FROM myView;
    """.trimMargin()
    )

    myFixture.renameElementAtCaret("newAlias")
    myFixture.checkResult(
      """
      |CREATE VIEW myView AS
      |SELECT 1 AS newAlias;
      |
      |someSelect:
      |SELECT newAlias
      |FROM myView;
    """.trimMargin()
    )
  }

  fun testTableAliasRename() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE VIEW myView AS
      |SELECT 1 AS columnAlias;
      |
      |someSelect:
      |SELECT tableAlias.columnAlias
      |FROM myView tableAlias
      |WHERE <caret>tableAlias.columnAlias = 1;
    """.trimMargin()
    )

    myFixture.renameElementAtCaret("newAlias")
    myFixture.checkResult(
      """
      |CREATE VIEW myView AS
      |SELECT 1 AS columnAlias;
      |
      |someSelect:
      |SELECT newAlias.columnAlias
      |FROM myView newAlias
      |WHERE newAlias.columnAlias = 1;
    """.trimMargin()
    )
  }

  fun testCteTableRename() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE VIEW myView AS
      |SELECT 1 AS columnAlias;
      |
      |someSelect:
      |WITH tableAlias AS (
      |  SELECT * FROM myView
      |)
      |SELECT tableAlias.columnAlias
      |FROM tableAlias
      |WHERE <caret>tableAlias.columnAlias = 1;
    """.trimMargin()
    )

    myFixture.renameElementAtCaret("newAlias")
    myFixture.checkResult(
      """
      |CREATE VIEW myView AS
      |SELECT 1 AS columnAlias;
      |
      |someSelect:
      |WITH newAlias AS (
      |  SELECT * FROM myView
      |)
      |SELECT newAlias.columnAlias
      |FROM newAlias
      |WHERE newAlias.columnAlias = 1;
    """.trimMargin()
    )
  }

  fun testCteColumnRename() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |someSelect:
      |WITH test (columnAlias) AS (
      |  SELECT 1
      |)
      |SELECT test.columnAlias
      |FROM test
      |WHERE test.<caret>columnAlias = 1;
    """.trimMargin()
    )

    myFixture.renameElementAtCaret("newAlias")
    myFixture.checkResult(
      """
      |someSelect:
      |WITH test (newAlias) AS (
      |  SELECT 1
      |)
      |SELECT test.newAlias
      |FROM test
      |WHERE test.newAlias = 1;
    """.trimMargin()
    )
  }
}
