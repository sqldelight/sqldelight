package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper

class RedundantNullCheckInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "not-null-check"

  fun testUnusedImportInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(RedundantNullCheckInspection()))
  }
}
