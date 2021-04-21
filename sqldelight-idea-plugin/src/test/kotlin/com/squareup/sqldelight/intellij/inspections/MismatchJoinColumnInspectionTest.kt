package com.squareup.sqldelight.intellij.inspections

import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase

class MismatchJoinColumnInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "join-inspections"

  fun testUnusedImportInspection() {
    myFixture.testInspection("", LocalInspectionToolWrapper(MismatchJoinColumnInspection()))
  }
}
