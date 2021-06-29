package com.squareup.sqldelight.intellij.inspections

import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase

class RedundantNullCheckInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "not-null-check"

  fun testUnusedImportInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(RedundantNullCheckInspection()))
  }
}
