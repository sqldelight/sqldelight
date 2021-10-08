package com.squareup.sqldelight.intellij.inspections

import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase

class NullEqualityInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "null-equality"

  fun testNullEqualityInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(NullEqualityInspection()))
  }
}
