package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper

class UnusedQueryInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "unused-query-inspection"

  fun testInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(UnusedQueryInspection()))
  }
}
