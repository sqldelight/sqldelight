package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper

class MixedNamedAndPositionalParamsInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "mixed-named-positional-parameters"

  fun testMixedNamedAndPositionalParametersInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(MixedNamedAndPositionalParamsInspection()))
  }
}
