package com.squareup.sqldelight.intellij.inspections

import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase

class UnusedColumnInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "column-inspection"

  fun testUnusedColumnInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(UnusedColumnInspection()))
  }
}
