package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper

class MissingWhereInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "missing-where"

  fun testMismatchMissingWhereInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(MissingWhereInspection()))
  }
}
