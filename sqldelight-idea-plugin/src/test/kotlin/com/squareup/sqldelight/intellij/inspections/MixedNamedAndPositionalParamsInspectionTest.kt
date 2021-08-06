package com.squareup.sqldelight.intellij.inspections

import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase

class MixedNamedAndPositionalParamsInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "mixed-named-positional-parameters"

  fun testMixedNamedAndPositionalParametersInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(MixedNamedAndPositionalParamsInspection()))
  }
}
