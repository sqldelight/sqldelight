package app.cash.sqldelight.intellij.intentions

import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.google.common.truth.Truth.assertThat

class CreateViewIntentionTest : SqlDelightFixtureTestCase() {

  fun testIntentionAvailableOnSelectStmt() {
    myFixture.configureByText(
      SqlDelightFileType,
      CREATE_TABLE + """
        |SELECT column_1
        |FROM ta<caret>ble_1;
      """.trimMargin(),
    )

    val intention = CreateViewIntention()

    assertThat(
      intention.isAvailable(
        myFixture.project,
        myFixture.editor,
        myFixture.file.findElementAt(myFixture.editor.caretModel.offset)!!,
      ),
    )
      .isTrue()
  }

  fun testIntentionNotAvailableInsideCreateView() {
    myFixture.configureByText(
      SqlDelightFileType,
      CREATE_TABLE + """
        |SELECT * FROM some_view;
        |
        |CREATE VIEW some_view AS SE<caret>LECT * FROM table_1;
      """.trimMargin(),
    )

    val intention = CreateViewIntention()

    assertThat(
      intention.isAvailable(
        myFixture.project,
        myFixture.editor,
        myFixture.file.findElementAt(myFixture.editor.caretModel.offset)!!,
      ),
    )
      .isFalse()
  }

  fun testIntentionExecution() {
    myFixture.configureByText(
      SqlDelightFileType,
      CREATE_TABLE + """
        |select:
        |SELECT column_1
        |FROM table_1
        |WHERE column_1 = (
        |   SELECT co<caret>lumn_2 FROM table_2
        |);
      """.trimMargin(),
    )

    val intention = CreateViewIntention()
    intention.invoke(
      myFixture.project,
      myFixture.editor,
      myFixture.file.findElementAt(myFixture.editor.caretModel.offset)!!,
    )

    myFixture.checkResult(
      CREATE_TABLE + """
        |CREATE VIEW some_view AS SELECT column_2 FROM table_2;
        |
        |select:
        |SELECT column_1
        |FROM table_1
        |WHERE column_1 = (
        |   SELECT * FROM some_view
        |);
      """.trimMargin(),
    )
  }

  companion object {
    val CREATE_TABLE = """
      |CREATE TABLE table_1 (
      |  column_1 INTEGER NOT NULL
      |);
      |
      |CREATE TABLE table_2 (
      |  column_2 INTEGER NOT NULL
      |);
      |
    """.trimMargin()
  }
}
