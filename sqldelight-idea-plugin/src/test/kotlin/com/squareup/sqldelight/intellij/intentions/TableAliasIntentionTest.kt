package com.squareup.sqldelight.intellij.intentions

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase

class TableAliasIntentionTest : SqlDelightFixtureTestCase() {

  fun testIntentionAvailableOnTableName() {
    myFixture.configureByText(
      SqlDelightFileType,
      CREATE_TABLE + """
        |SELECT first_name, last_name, number, team, name
        |FROM hockey<caret>Player
        |JOIN team ON hockeyPlayer.team = team.id;
      """.trimMargin()
    )

    val intention = IntroduceTableAliasIntention()
    assertThat(
      intention.isAvailable(
        myFixture.project,
        myFixture.editor,
        myFixture.file.findElementAt(myFixture.editor.caretModel.offset)!!
      )
    )
      .isTrue()
  }

  fun testIntentionNotAvailableOnTableNameWithAlias() {
    myFixture.configureByText(
      SqlDelightFileType,
      CREATE_TABLE + """
        |SELECT first_name, last_name, number, team, name
        |FROM hockey<caret>Player hp
        |JOIN team ON hp.team = team.id;
      """.trimMargin()
    )

    val intention = IntroduceTableAliasIntention()
    assertThat(
      intention.isAvailable(
        myFixture.project,
        myFixture.editor,
        myFixture.file.findElementAt(myFixture.editor.caretModel.offset)!!
      )
    )
      .isFalse()
  }

  fun testIntentionNotAvailableOnColumnQualifier() {
    myFixture.configureByText(
      SqlDelightFileType,
      CREATE_TABLE + """
        |SELECT id, tea<caret>m.name
        |FROM team;
      """.trimMargin()
    )

    val intention = IntroduceTableAliasIntention()
    assertThat(
      intention.isAvailable(
        myFixture.project,
        myFixture.editor,
        myFixture.file.findElementAt(myFixture.editor.caretModel.offset)!!
      )
    )
      .isFalse()
  }

  fun testIntentionExecution() {
    myFixture.configureByText(
      SqlDelightFileType,
      CREATE_TABLE + """
        |SELECT first_name, last_name, number, team, name
        |FROM hockeyPlayer
        |JOIN tea<caret>m ON hockeyPlayer.team = team.id;
      """.trimMargin()
    )

    val intention = IntroduceTableAliasIntention()
    intention.invoke(
      myFixture.project,
      myFixture.editor,
      myFixture.file.findElementAt(myFixture.editor.caretModel.offset)!!
    )

    myFixture.checkResult(
      CREATE_TABLE + """
        |SELECT first_name, last_name, number, team, name
        |FROM hockeyPlayer
        |JOIN team t ON hockeyPlayer.team = t.id;
      """.trimMargin()
    )
  }

  companion object {
    private val CREATE_TABLE = """
      |CREATE TABLE hockeyPlayer(
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  first_name TEXT NOT NULL,
      |  last_name TEXT NOT NULL,
      |  number INTEGER NOT NULL,
      |  team INTEGER NOT NULL,
      |  FOREIGN KEY (team) REFERENCES team(id)
      |);
      |
      |CREATE TABLE team(
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  name TEXT NOT NULL UNIQUE
      |);
      |""".trimMargin()
  }
}
