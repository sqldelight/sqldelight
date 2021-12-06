package app.cash.sqldelight.intellij.intentions

import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.google.common.truth.Truth.assertThat

class ExpandColumnNamesWildcardQuickFixTest : SqlDelightFixtureTestCase() {

  fun testIntentionAvailableOnWildcard() {
    myFixture.configureByText(
      SqlDelightFileType,
      CREATE_TABLE + """
      |select_all:
      |SELECT <caret>*
      |FROM test;
      """.trimMargin()
    )

    val intention = ExpandColumnNamesWildcardQuickFix()
    assertThat(myFixture.availableIntentions.firstOrNull { it.text == intention.text })
      .isNotNull()
  }

  fun testIntentionNotAvailableOnColumnName() {
    myFixture.configureByText(
      SqlDelightFileType,
      CREATE_TABLE + """
      |select_all:
      |SELECT <caret>_id
      |FROM test;
      """.trimMargin()
    )

    val intention = ExpandColumnNamesWildcardQuickFix()
    assertThat(myFixture.availableIntentions.firstOrNull { it.text == intention.text })
      .isNull()
  }

  fun testExecuteIntention() {
    myFixture.configureByText(
      SqlDelightFileType,
      CREATE_TABLE + """
      |select_all:
      |SELECT <caret>*
      |FROM test;
      """.trimMargin()
    )

    val intention = ExpandColumnNamesWildcardQuickFix()
    intention.invoke(myFixture.project, myFixture.editor, myFixture.file)

    myFixture.checkResult(
      CREATE_TABLE + """
      |select_all:
      |SELECT <caret>_id, title
      |FROM test;
      """.trimMargin()
    )
  }

  fun testIntentionAvailableOnWildcardWithAdditionalProjections() {
    myFixture.configureByText(
      SqlDelightFileType,
      CREATE_TABLE + """
      |select_all:
      |SELECT <caret>*, 1
      |FROM test;
      """.trimMargin()
    )

    val intention = ExpandColumnNamesWildcardQuickFix()
    assertThat(myFixture.availableIntentions.firstOrNull { it.text == intention.text })
      .isNotNull()
  }

  fun testIntentionNotAvailableOnWildcardInsideFunctionExpr() {
    myFixture.configureByText(
      SqlDelightFileType,
      CREATE_TABLE + """
      |select_all:
      |SELECT COUNT(<caret>*)
      |FROM test;
      """.trimMargin()
    )

    val intention = ExpandColumnNamesWildcardQuickFix()
    assertThat(myFixture.availableIntentions.firstOrNull { it.text == intention.text })
      .isNull()
  }

  fun testExecuteIntentionWithAdditionalProjections() {
    myFixture.configureByText(
      SqlDelightFileType,
      CREATE_TABLE + """
      |select_all:
      |SELECT <caret>*, 1, COUNT(*), title
      |FROM test;
      """.trimMargin()
    )

    val intention = ExpandColumnNamesWildcardQuickFix()
    intention.invoke(myFixture.project, myFixture.editor, myFixture.file)

    myFixture.checkResult(
      CREATE_TABLE + """
      |select_all:
      |SELECT <caret>_id, title, 1, COUNT(*)
      |FROM test;
      """.trimMargin()
    )
  }

  companion object {
    private val CREATE_TABLE = """
      |CREATE TABLE test (
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  title TEXT NOT NULL
      |);
      |""".trimMargin()
  }
}
