package com.squareup.sqldelight.intellij.autocomplete

import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.intellij.SqlDelightProjectTestCase

class ClassAutocompleteTests : SqlDelightProjectTestCase() {
  fun testClassAutocompleteFindsLocalKotlinClasses() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  value TEXT AS Ko<caret>
      |);
    """.trimMargin())

    myFixture.completeBasic()

    myFixture.checkResult("""
      |import com.example.KotlinClass;
      |
      |CREATE TABLE test (
      |  value TEXT AS KotlinClass
      |);
    """.trimMargin())
  }

  fun testUnresolvedClassName() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  value TEXT AS <error descr="Unresolved reference: KoolKidz">KoolKidz</error>
      |);
    """.trimMargin())

    myFixture.checkHighlighting()
  }

  fun testInnerClassWorksFine() {
    myFixture.configureByText(SqlDelightFileType, """
      |import com.example.KotlinClass;
      |
      |CREATE TABLE test (
      |  value TEXT AS KotlinClass.InnerClass
      |);
    """.trimMargin())

    myFixture.checkHighlighting()
  }
}