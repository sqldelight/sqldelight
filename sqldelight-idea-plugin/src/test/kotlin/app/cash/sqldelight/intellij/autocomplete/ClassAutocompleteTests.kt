package app.cash.sqldelight.intellij.autocomplete

import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.intellij.SqlDelightProjectTestCase

class ClassAutocompleteTests : SqlDelightProjectTestCase() {
  // 2018.1 Broke JavaAutocompletion as the module source root is incorrectly set up
  fun ignoreTestClassAutocompleteFindsLocalKotlinClasses() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE test (
      |  value TEXT AS Ko<caret>
      |);
    """.trimMargin()
    )

    myFixture.completeBasic()

    myFixture.checkResult(
      """
      |import com.example.KotlinClass;
      |
      |CREATE TABLE test (
      |  value TEXT AS KotlinClass
      |);
    """.trimMargin()
    )
  }

  fun testUnresolvedClassName() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE test (
      |  value TEXT AS <error descr="Unresolved reference: KoolKidz">KoolKidz</error>
      |);
    """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testInnerClassWorksFine() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import com.example.KotlinClass;
      |
      |CREATE TABLE test (
      |  value TEXT AS KotlinClass.InnerClass
      |);
    """.trimMargin()
    )

    myFixture.checkHighlighting()
  }
}
