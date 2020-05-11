package com.squareup.sqldelight.intellij

import com.alecstrong.sql.psi.core.psi.SqlColumnAlias
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlTableAlias
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.SqlViewName
import com.google.common.truth.Truth.assertThat
import com.intellij.psi.PsiReferenceExpression
import com.intellij.usageView.UsageInfo
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin
import org.jetbrains.kotlin.idea.findUsages.KotlinReferenceUsageInfo
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class FindUsagesTest : SqlDelightProjectTestCase() {
  // With the kotlin 1.3.30 plugin the test breaks. Find usages still works. Investigate later.
  fun ignoretestFindsBothKotlinAndJavaUsages() {
    myFixture.openFileInEditor(
        tempRoot.findFileByRelativePath("src/main/java/com/example/SampleClass.java")!!
    )
    val javaCallsite = searchForElement<PsiReferenceExpression>("someQuery").single()

    myFixture.openFileInEditor(
        tempRoot.findFileByRelativePath("src/main/kotlin/com/example/KotlinClass.kt")!!
    )
    val kotlinCallsite = searchForElement<KtReferenceExpression>("someQuery").single()

    myFixture.openFileInEditor(
        tempRoot.findFileByRelativePath("src/main/sqldelight/com/example/Main.sq")!!
    )
    val identifier = searchForElement<StmtIdentifierMixin>("someQuery").single()
    assertThat(myFixture.findUsages(identifier)).containsExactly(
        KotlinReferenceUsageInfo(javaCallsite),
        KotlinReferenceUsageInfo(kotlinCallsite.references.firstIsInstance())
    )
  }

  // With the kotlin 1.3.30 plugin the test breaks. Find usages still works. Investigate later.
  fun ignoretestFindsUsagesOfAllGeneratedMethods() {
    myFixture.openFileInEditor(
        tempRoot.findFileByRelativePath("src/main/kotlin/com/example/KotlinClass.kt")!!
    )
    val callsites = searchForElement<KtReferenceExpression>("multiQuery")

    myFixture.openFileInEditor(
        tempRoot.findFileByRelativePath("src/main/sqldelight/com/example/Main.sq")!!
    )
    val identifier = searchForElement<StmtIdentifierMixin>("multiQuery").single()
    assertThat(myFixture.findUsages(identifier)).containsExactly(*callsites.map {
      KotlinReferenceUsageInfo(it.references.firstIsInstance())
    }.toTypedArray())
  }

  fun testFindsUsagesOfTable() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  value TEXT NOT NULL
      |);
      |
      |INSERT INTO test
      |VALUES ('stuff');
      |
      |someSelect:
      |SELECT *
      |FROM test;
    """.trimMargin())
    val tableName = searchForElement<SqlTableName>("test")
    assertThat(tableName).hasSize(3)

    assertThat(myFixture.findUsages(tableName.first())).containsExactly(*tableName.map {
      UsageInfo(it)
    }.toTypedArray())
  }

  fun testFindsUsagesOfColumn() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  value TEXT NOT NULL
      |);
      |
      |INSERT INTO test (value)
      |VALUES ('stuff');
      |
      |anUpdate:
      |UPDATE test
      |SET value = ?;
      |
      |someSelect:
      |SELECT value
      |FROM test
      |WHERE test.value = ?;
    """.trimMargin())
    val tableName = searchForElement<SqlColumnName>("value")
    assertThat(tableName).hasSize(5)

    assertThat(myFixture.findUsages(tableName.first())).containsExactly(*tableName.map {
      UsageInfo(it)
    }.toTypedArray())
  }

  fun testFindsUsagesOfView() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE VIEW test AS
      |SELECT 1, 2;
      |
      |someSelect:
      |SELECT *
      |FROM test;
    """.trimMargin())
    val viewName = searchForElement<SqlViewName>("test") +
        searchForElement<SqlTableName>("test")
    assertThat(viewName).hasSize(2)

    assertThat(myFixture.findUsages(viewName.first())).containsExactly(*viewName.map {
      UsageInfo(it)
    }.toTypedArray())
  }

  fun testFindsUsagesOfColumnAlias() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  stuff TEXT NOT NULL
      |);
      |
      |CREATE VIEW test_view AS
      |SELECT stuff AS stuff_alias
      |FROM test;
      |
      |someSelect:
      |SELECT stuff_alias
      |FROM test_view;
    """.trimMargin())
    val columnAlias = searchForElement<SqlColumnAlias>("stuff_alias") +
        searchForElement<SqlColumnName>("stuff_alias")
    assertThat(columnAlias).hasSize(2)

    assertThat(myFixture.findUsages(columnAlias.first())).containsExactly(*columnAlias.drop(1).map {
      UsageInfo(it)
    }.toTypedArray())
  }

  fun testFindsUsagesOfTableAlias() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  stuff TEXT NOT NULL
      |);
      |
      |someSelect:
      |SELECT test_alias.*
      |FROM test test_alias
      |WHERE test_alias.stuff = ?;
    """.trimMargin())
    val tableAlias = searchForElement<SqlTableAlias>("test_alias") +
        searchForElement<SqlTableName>("test_alias")
    assertThat(tableAlias).hasSize(3)

    assertThat(myFixture.findUsages(tableAlias.first())).containsExactly(*tableAlias.drop(1).map {
      UsageInfo(it)
    }.toTypedArray())
  }

  fun testFindsUsagesCommonTableName() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  stuff TEXT NOT NULL
      |);
      |
      |someSelect:
      |WITH test_alias AS (
      |  SELECT *
      |  FROM test
      |)
      |SELECT test_alias.*
      |FROM test_alias
      |WHERE test_alias.stuff = ?;
    """.trimMargin())
    val tableName = searchForElement<SqlTableName>("test_alias")
    assertThat(tableName).hasSize(4)

    assertThat(myFixture.findUsages(tableName.first())).containsExactly(*tableName.map {
      UsageInfo(it)
    }.toTypedArray())
  }

  fun testFindsUsagesCommonTableColumnAlias() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  stuff TEXT NOT NULL
      |);
      |
      |someSelect:
      |WITH test_alias (stuff_alias) AS (
      |  SELECT *
      |  FROM test
      |)
      |SELECT stuff_alias
      |FROM test_alias
      |WHERE test_alias.stuff_alias = ?;
    """.trimMargin())
    val columnAlias = searchForElement<SqlColumnAlias>("stuff_alias") +
        searchForElement<SqlColumnName>("stuff_alias")
    assertThat(columnAlias).hasSize(3)

    assertThat(myFixture.findUsages(columnAlias.first())).containsExactly(*columnAlias.drop(1).map {
      UsageInfo(it)
    }.toTypedArray())
  }
}
