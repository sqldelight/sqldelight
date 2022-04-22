package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper

class IncorrectSelectTableInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "incorrect-select-table"

  fun testIncorrectSelectTableInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(IncorrectSelectTableInspection()))
  }
}
