package com.squareup.sqldelight.intellij.lang

import app.cash.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand

class SqlDelightImportOptimizerTest : SqlDelightFixtureTestCase() {

  fun testImportOptimizer() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import java.lang.Integer;
      |import kotlin.collections.List;
      |import org.jetbrains.annotations.Nullable;
      |
      |CREATE TABLE hockeyPlayer (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  first_name TEXT NOT NULL,
      |  last_name TEXT AS @Nullable String NOT NULL,
      |  list TEXT AS List<Int>
      |);
    """.trimMargin()
    )

    project.executeWriteCommand("") {
      SqlDelightImportOptimizer().processFile(myFixture.file).run()
    }

    myFixture.checkResult(
      """
      |import kotlin.collections.List;
      |import org.jetbrains.annotations.Nullable;
      |
      |CREATE TABLE hockeyPlayer (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  first_name TEXT NOT NULL,
      |  last_name TEXT AS @Nullable String NOT NULL,
      |  list TEXT AS List<Int>
      |);
    """.trimMargin()
    )
  }
}
