package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper

class NullEqualityInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "null-equality"

  fun testNullEqualityInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(NullEqualityInspection()))
  }
}
