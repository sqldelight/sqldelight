package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper

class UnusedQueryInspectionTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "unused-query-inspection"

  fun testInspection() {
    myFixture.copyFileToProject("Main.kt", "main/kotlin/com/example/Main.kt")
    myFixture.copyFileToProject("SomeTableQueries.kt", "build/com/example/SomeTableQueries.kt")
    myFixture.copyFileToProject("Example.kt", "build/com/example/Example.kt")
    myFixture.copyFileToProject("Query.kt", "build/com/example/Query.kt")

    myFixture.testInspection("", LocalInspectionToolWrapper(UnusedQueryInspection()))
  }
}
