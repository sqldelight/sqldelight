package app.cash.sqldelight.intellij

import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.google.common.truth.Truth.assertThat

class SqlDelightIdentifierHandlerTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "find-usages"

  fun testFindColumnUsages() {
    myFixture.configureByFile("Example.sq")
    myFixture.copyFileToProject("Main.kt", "main/kotlin/com/example/Main.kt")
    myFixture.copyFileToProject("Example.kt", "build/com/example/Example.kt")
    myFixture.copyFileToProject("ExampleQueries.kt", "build/com/example/ExampleQueries.kt")
    myFixture.copyFileToProject("Query.kt", "build/com/example/Query.kt")
    val sqlColumnName = myFixture.findElementByText("id", SqlColumnName::class.java)

    val usageInfos = myFixture.findUsages(sqlColumnName)

    assertThat(usageInfos.size).isEqualTo(6)
  }
}
