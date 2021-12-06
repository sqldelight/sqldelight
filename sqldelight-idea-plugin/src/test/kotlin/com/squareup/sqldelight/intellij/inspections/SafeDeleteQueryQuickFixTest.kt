package com.squareup.sqldelight.intellij.inspections

import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin
import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase

class SafeDeleteQueryQuickFixTest : SqlDelightFixtureTestCase() {

  fun testSafeDeleteQuery() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE hockeyPlayer(
      | player_number INTEGER NOT NULL PRIMARY KEY,
      | full_name TEXT NOT NULL
      |);
      |
      |selectAll:
      |SELECT * FROM hockeyPlayer;
      |
      |selectNumber:
      |SELECT player_number FROM hockeyPlayer;
    """.trimMargin()
    )

    val file = myFixture.file
    val queryStmt = file.findChildrenOfType<StmtIdentifierMixin>()
      .first { it.textMatches("selectAll:") }
    val qf = UnusedQueryInspection.SafeDeleteQuickFix(queryStmt)

    qf.invoke(project, file, queryStmt, queryStmt)

    myFixture.checkResult(
      """
      |CREATE TABLE hockeyPlayer(
      | player_number INTEGER NOT NULL PRIMARY KEY,
      | full_name TEXT NOT NULL
      |);
      |
      |selectNumber:
      |SELECT player_number FROM hockeyPlayer;
    """.trimMargin()
    )
  }
}
