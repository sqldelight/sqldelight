package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper

class MismatchJoinColumnInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "join-inspections"

  fun testMismatchJoinColumnInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(MismatchJoinColumnInspection()))
  }
}
