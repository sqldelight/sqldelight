package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.core.lang.util.findChildOfType
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt

class SafeDeleteColumnQuickFixTest : SqlDelightFixtureTestCase() {

  fun testMultipleColumnsFirstLine() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE hockeyPlayer(
      |  player_number INTEGER NOT NULL PRIMARY KEY,
      |  full_name TEXT NOT NULL
      |);
    """.trimMargin()
    )

    val file = myFixture.file
    val createTableStmt = file.findChildOfType<SqlCreateTableStmt>()!!
    val columnDef = file.findChildrenOfType<SqlColumnDef>()
      .first { it.columnName.textMatches("player_number") }
    val qf = UnusedColumnInspection.SafeDeleteQuickFix(createTableStmt, columnDef)

    qf.invoke(project, file, createTableStmt, createTableStmt)

    myFixture.checkResult(
      """
      |CREATE TABLE hockeyPlayer(
      |  full_name TEXT NOT NULL
      |);
    """.trimMargin()
    )
  }

  fun testMultipleColumnsLastLine() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE hockeyPlayer(
      |  player_number INTEGER NOT NULL PRIMARY KEY,
      |  full_name TEXT NOT NULL
      |);
    """.trimMargin()
    )

    val file = myFixture.file
    val createTableStmt = file.findChildOfType<SqlCreateTableStmt>()!!
    val columnDef = file.findChildrenOfType<SqlColumnDef>()
      .first { it.columnName.textMatches("full_name") }
    val qf = UnusedColumnInspection.SafeDeleteQuickFix(createTableStmt, columnDef)

    qf.invoke(project, file, createTableStmt, createTableStmt)

    myFixture.checkResult(
      """
      |CREATE TABLE hockeyPlayer(
      |  player_number INTEGER NOT NULL PRIMARY KEY
      |);
    """.trimMargin()
    )
  }

  fun testSingleColumn() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE hockeyPlayer(
      |  player_number INTEGER NOT NULL PRIMARY KEY
      |);
    """.trimMargin()
    )

    val file = myFixture.file
    val createTableStmt = file.findChildOfType<SqlCreateTableStmt>()!!
    val columnDef = file.findChildrenOfType<SqlColumnDef>()
      .first { it.columnName.textMatches("player_number") }
    val qf = UnusedColumnInspection.SafeDeleteQuickFix(createTableStmt, columnDef)

    qf.invoke(project, file, createTableStmt, createTableStmt)

    myFixture.checkResult(
      """
      |CREATE TABLE hockeyPlayer();
    """.trimMargin()
    )
  }
}
