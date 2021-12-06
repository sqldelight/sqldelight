package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper

class UnusedImportInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "import-inspection"

  fun testUnusedImportInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(UnusedImportInspection()))
  }
}
