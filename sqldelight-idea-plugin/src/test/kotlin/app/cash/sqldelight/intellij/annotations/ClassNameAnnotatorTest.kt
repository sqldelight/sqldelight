package app.cash.sqldelight.intellij.annotations

import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.intellij.SqlDelightProjectTestCase
import org.jetbrains.kotlin.idea.KotlinFileType

class ClassNameAnnotatorTest : SqlDelightProjectTestCase() {
  fun testResolveOnSamePackageImport() {
    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/sqldelight/com/example/Main.sq")!!
    )

    myFixture.checkHighlighting()
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

  fun testUnresolvedImport() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import <error descr="Unresolved reference: com.somepackage.SomeClass">com.somepackage.SomeClass</error>;
      |
      |CREATE TABLE new_table (
      |  col TEXT AS <error descr="Unresolved reference: SomeClass">SomeClass</error> NOT NULL
      |);
    """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testUnresolvedNestedClass() {
    myFixture.configureByText(
      KotlinFileType.INSTANCE,
      """
        |package com.somepackage
        |
        |class SomeClass {
        |  class FirstLevel
        |}
      """.trimMargin()
    )

    myFixture.configureByText(
      SqlDelightFileType,
      """
        |import com.somepackage.SomeClass;
        |
        |CREATE TABLE new_table (
        |  col TEXT AS SomeClass.FirstLevel.<error descr="Unresolved reference: SecondLevel">SecondLevel</error> NOT NULL
        |);
        """.trimMargin()
    )

    myFixture.checkHighlighting()
  }
}
