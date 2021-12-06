package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper

class UnusedColumnInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "column-inspection"

  fun testUnusedColumnInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(UnusedColumnInspection()))
  }
}
