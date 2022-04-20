package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.google.common.truth.Truth.assertThat
import com.intellij.icons.AllIcons

class RunSqliteAnnotatorTest : SqlDelightFixtureTestCase() {

  fun testGutterIconVisibleInSqlite() {
    ConnectionOptions(project).apply {
      connectionType = ConnectionType.FILE
      filePath = "/path/to/file"
    }
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE test ( <caret>
      | id INTEGER
      |);
    """.trimMargin()
    )

    val guttersAtCaret = myFixture.findGuttersAtCaret()
    assertThat(guttersAtCaret.first().icon).isEqualTo(AllIcons.RunConfigurations.TestState.Run)
  }

  fun testGutterIconInvisibleWhenConnectionTypeNotSpecified() {
    ConnectionOptions(project).apply {
      connectionType = ConnectionType.NONE
    }
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE test ( <caret>
      | id INTEGER
      |);
    """.trimMargin()
    )
    assertThat(myFixture.findGuttersAtCaret()).isEmpty()
  }
}
