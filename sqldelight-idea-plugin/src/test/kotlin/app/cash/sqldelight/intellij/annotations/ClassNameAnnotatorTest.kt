package app.cash.sqldelight.intellij.annotations

import app.cash.sqldelight.intellij.SqlDelightProjectTestCase

class ClassNameAnnotatorTest : SqlDelightProjectTestCase() {
  fun testResolveOnSamePackageImport() {
    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/sqldelight/com/example/Main.sq")!!
    )

    myFixture.checkHighlighting()
  }
}
