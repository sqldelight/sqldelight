package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.lang.SqlDelightFileType
import com.google.common.truth.Truth.assertThat
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.plugins.groovy.lang.psi.util.childrenOfType

class ReferenceContrubutorTest : SqlDelightFixtureTestCase() {

  fun testJavaClassImportResolvesCorrectly() {
    myFixture.configureByText(
      "SomeClass.java",
      """
        |package com.somepackage;
        |
        |public class SomeClass {
        |}
      """.trimMargin()
    )
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import com.somepackage.S<caret>omeClass;
      |
      |CREATE TABLE new_table (
      |  col TEXT AS SomeClass NOT NULL
      |);
      """.trimMargin()
    )
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    val psiClass = myFixture.findClass("com.somepackage.SomeClass")
    assertThat(reference.isReferenceTo(psiClass)).isTrue()
  }

  fun testImportedJavaClassResolvesCorrectly() {
    myFixture.configureByText(
      "SomeClass.java",
      """
        |package com.somepackage;
        |
        |public class SomeClass {
        |}
      """.trimMargin()
    )
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import com.somepackage.SomeClass;
      |
      |CREATE TABLE new_table (
      |  col TEXT AS Some<caret>Class NOT NULL
      |);
      """.trimMargin()
    )
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    val psiClass = myFixture.findClass("com.somepackage.SomeClass")
    assertThat(reference.isReferenceTo(psiClass)).isTrue()
  }

  fun testKotlinClassImportResolvesCorrectly() {
    myFixture.configureByText(
      "SomeClass.kt",
      """
      |package com.somepackage
      |
      |class SomeClass
    """.trimMargin()
    )
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import com.somepackage.S<caret>omeClass;
      |
      |CREATE TABLE new_table (
      |  col TEXT AS SomeClass NOT NULL
      |);
      """.trimMargin()
    )
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    val psiClass = myFixture.findClass("com.somepackage.SomeClass")
    assertThat(reference.isReferenceTo(psiClass)).isTrue()
  }

  fun testImportedKotlinClassResolvesCorrectly() {
    myFixture.configureByText(
      "SomeClass.kt",
      """
      |package com.somepackage
      |
      |class SomeClass
    """.trimMargin()
    )
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import com.somepackage.SomeClass;
      |
      |CREATE TABLE new_table (
      |  col TEXT AS Some<caret>Class NOT NULL
      |);
      """.trimMargin()
    )
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    val psiClass = myFixture.findClass("com.somepackage.SomeClass")
    assertThat(reference.isReferenceTo(psiClass)).isTrue()
  }

  fun testExpectClassImportResolvesCorrectly() {
    val psiFile = myFixture.configureByText(
      "SomeExpectClass.kt",
      """
      |package com.somepackage
      |
      |expect class SomeExpectClass
    """.trimMargin()
    )
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import com.somepackage.SomeExpect<caret>Class;
      |
      |CREATE TABLE new_table (
      |  col TEXT AS SomeExpectClass NOT NULL
      |);
      """.trimMargin()
    )
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    val psiClass = psiFile.childrenOfType<KtClassOrObject>().single()
    assertThat(reference.isReferenceTo(psiClass)).isTrue()
  }

  fun testImportedExpectClassResolvesCorrectly() {
    val file = myFixture.configureByText(
      "SomeExpectClass.kt",
      """
      |package com.somepackage
      |
      |expect class SomeExpectClass
    """.trimMargin()
    )
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import com.somepackage.SomeExpectClass;
      |
      |CREATE TABLE new_table (
      |  col TEXT AS SomeExpect<caret>Class NOT NULL
      |);
      """.trimMargin()
    )
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    val psiClass = file.childrenOfType<KtClassOrObject>().single()
    assertThat(reference.isReferenceTo(psiClass)).isTrue()
  }

  fun testKotlinPrimitiveTypeImportResolvesCorrectly() {
    val file = myFixture.configureByText(
      "Int.kt",
      """
        |package kotlin
        |
        |class Int
      """.trimMargin()
    )
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import kotlin.I<caret>nt;
      |
      |CREATE TABLE new_table (
      |  col TEXT AS Int NOT NULL
      |);
      """.trimMargin()
    )
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    val psiClass = file.childrenOfType<KtClassOrObject>().single()
    assertThat(reference.isReferenceTo(psiClass)).isTrue()
  }

  fun testImportedPrimitiveTypeResolvesCorrectly() {
    val file = myFixture.configureByText(
      "Int.kt",
      """
        |package kotlin
        |
        |class Int
      """.trimMargin()
    )
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import kotlin.Int;
      |
      |CREATE TABLE new_table (
      |  col TEXT AS I<caret>nt NOT NULL
      |);
      """.trimMargin()
    )
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    val psiClass = file.childrenOfType<KtClassOrObject>().single()
    assertThat(reference.isReferenceTo(psiClass)).isTrue()
  }

  fun testSamePackageClassResolvesWithoutImport() {
    val file = myFixture.configureByText(
      "SomeClass.kt",
      """
        |package com.example
        |
        |class SomeClass
      """.trimMargin()
    )
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE new_table (
      |  col TEXT AS Some<caret>Class NOT NULL
      |);
      """.trimMargin()
    )
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    val psiClass = file.childrenOfType<KtClassOrObject>().single()
    assertThat(reference.isReferenceTo(psiClass)).isTrue()
  }

  fun testUnimportedClassDoesntResolve() {
    myFixture.configureByText(
      "SomeClass.kt",
      """
        |package com.somepackage
        |
        |class SomeClass
      """.trimMargin()
    )
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE new_table (
      |  col TEXT AS Some<caret>Class NOT NULL
      |);
      """.trimMargin()
    )
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    assertThat(reference.resolve()).isNull()
  }

  fun testNestedKotlinClassResolvesCorrectly() {
    myFixture.configureByText(
      "SomeClass.kt",
      """
        |package com.somepackage
        |
        |class SomeClass {
        |  class NestedClass
        |}
      """.trimMargin()
    )
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import com.somepackage.SomeClass;
      |
      |CREATE TABLE new_table (
      |  col TEXT AS SomeClass.Nes<caret>tedClass NOT NULL
      |);
      """.trimMargin()
    )
    val psiClass = myFixture.findClass("com.somepackage.SomeClass.NestedClass")
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    assertThat(reference.isReferenceTo(psiClass)).isTrue()
  }

  fun testImportedTypeAliasResolvesCorrectly() {
    val file = myFixture.configureByText(
      "SomeType.kt",
      """
      |package com.somepackage
      |
      |typealias SomeType = Int
    """.trimMargin()
    )
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import com.somepackage.SomeType;
      |
      |CREATE TABLE new_table (
      |  col TEXT AS Some<caret>Type NOT NULL
      |);
      """.trimMargin()
    )
    val typeAlias = file.childrenOfType<KtTypeAlias>().single().psiOrParent
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    assertThat(reference.isReferenceTo(typeAlias)).isTrue()
  }

  fun testImportedEnumResolvesCorrectly() {
    myFixture.configureByText(
      "SomeEnum.kt",
      """
      |package com.somepackage
      |
      |enum class SomeEnum
    """.trimMargin()
    )
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |import com.somepackage.SomeEnum;
      |
      |CREATE TABLE new_table (
      |  col TEXT AS Some<caret>Enum NOT NULL
      |);
      """.trimMargin()
    )
    val psiClass = myFixture.findClass("com.somepackage.SomeEnum")
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    assertThat(reference.isReferenceTo(psiClass)).isTrue()
  }
}
