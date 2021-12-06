package app.cash.sqldelight.intellij.rename

import app.cash.sqldelight.core.lang.psi.StmtIdentifierMixin
import app.cash.sqldelight.intellij.SqlDelightProjectTestCase
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlViewName
import com.google.common.truth.Truth.assertThat
import com.intellij.psi.PsiElement

class SqlStmtIdentifierTests : SqlDelightProjectTestCase() {
  fun testRenamingIdentifierRenamesKotlinAndJavaUsages() {
    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/sqldelight/com/example/Main.sq")!!
    )
    val identifier = searchForElement<StmtIdentifierMixin>("someQuery").single()

    myFixture.renameElement(identifier, "newSomeQuery")

    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/java/com/example/SampleClass.java")!!
    )
    assertThat(searchForElement<PsiElement>("newSomeQuery")).hasSize(1)
    assertThat(searchForElement<PsiElement>("someQuery")).isEmpty()

    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/kotlin/com/example/KotlinClass.kt")!!
    )
    assertThat(searchForElement<PsiElement>("newSomeQuery")).hasSize(1)
    assertThat(searchForElement<PsiElement>("someQuery")).isEmpty()
  }

  fun testRenamingQueryWithMultipleMethods() {
    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/sqldelight/com/example/Main.sq")!!
    )
    val identifier = searchForElement<StmtIdentifierMixin>("multiQuery").single()

    myFixture.renameElement(identifier, "newMultiQuery")

    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/kotlin/com/example/KotlinClass.kt")!!
    )
    assertThat(searchForElement<PsiElement>("newMultiQuery")).hasSize(2)
    assertThat(searchForElement<PsiElement>("multiQuery")).isEmpty()
  }

  fun testRenamingQueryWithCustomTypes() {
    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/sqldelight/com/example/Main.sq")!!
    )
    val identifier = searchForElement<StmtIdentifierMixin>("generatesType").single()
    myFixture.renameElement(identifier, "newGeneratesType")

    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/kotlin/com/example/GeneratesTypeImpl.kt")!!
    )
    assertThat(searchForElement<PsiElement>("NewGeneratesType")).hasSize(1)
    assertThat(searchForElement<PsiElement>("GeneratesType")).isEmpty()

    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/kotlin/com/example/KotlinClass.kt")!!
    )
    assertThat(searchForElement<PsiElement>("NewGeneratesType")).hasSize(1)
    assertThat(searchForElement<PsiElement>("GeneratesType")).isEmpty()

    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/java/com/example/SampleClass.java")!!
    )
    assertThat(searchForElement<PsiElement>("NewGeneratesType")).hasSize(1)
    assertThat(searchForElement<PsiElement>("GeneratesType")).isEmpty()
  }

  fun testRenamingTableName() {
    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/sqldelight/com/example/Main.sq")!!
    )
    val identifier = searchForElement<SqlTableName>("main").first()
    myFixture.renameElement(identifier, "newMain")

    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/kotlin/com/example/MainImpl.kt")!!
    )
    assertThat(searchForElement<PsiElement>("NewMain")).hasSize(1)
    assertThat(searchForElement<PsiElement>("Main")).isEmpty()
  }

  fun testRenamingViewName() {
    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/sqldelight/com/example/Main.sq")!!
    )
    val identifier = searchForElement<SqlViewName>("someView").first()
    myFixture.renameElement(identifier, "newView")

    myFixture.openFileInEditor(
      tempRoot.findFileByRelativePath("src/main/kotlin/com/example/ViewImpl.kt")!!
    )
    assertThat(searchForElement<PsiElement>("NewView")).hasSize(1)
    assertThat(searchForElement<PsiElement>("SomeView")).isEmpty()
  }
}
