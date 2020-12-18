package com.squareup.sqldelight.intellij.annotations

import com.squareup.sqldelight.intellij.SqlDelightProjectTestCase

class ClassNameAnnotatorTest : SqlDelightProjectTestCase() {
  fun testResolveOnSamePackageImport() {
    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/sqldelight/com/example/Main.sq")!!
    )

    myFixture.checkHighlighting()
  }
}
