package app.cash.sqldelight.intellij.lang.completion

import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.intellij.SqlDelightFixtureTestCase
import com.google.common.truth.Truth.assertThat
import com.intellij.codeInsight.completion.CompletionType

class SqlDelightKeywordCompletionContributorTest : SqlDelightFixtureTestCase() {

  fun testJoinClauseCompletion() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE album(
      |  albumartist TEXT,
      |  albumname TEXT,
      |  albumcover TEXT,
      |  PRIMARY KEY(albumartist)
      |);
      |
      |CREATE TABLE song(
      |  songid     INTEGER,
      |  songartist TEXT,
      |  songalbum TEXT,
      |  songname   TEXT,
      |  FOREIGN KEY(songartist) REFERENCES album(albumartist)
      |);
      |
      |SELECT * FROM song JOIN <caret>
      """.trimMargin(),
    )

    myFixture.complete(CompletionType.BASIC)

    val lookupElementStrings = myFixture.lookupElementStrings
    assertThat(lookupElementStrings).contains("album ON song.songartist = album.albumartist")
  }

  fun testJoinClauseCompletionWithCompositeForeignKey() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
      |CREATE TABLE album(
      |  albumartist TEXT,
      |  albumname TEXT,
      |  albumcover TEXT,
      |  PRIMARY KEY(albumartist, albumname)
      |);
      |
      |CREATE TABLE song(
      |  songid     INTEGER,
      |  songartist TEXT,
      |  songalbum TEXT,
      |  songname   TEXT,
      |  FOREIGN KEY(songartist, songalbum) REFERENCES album(albumartist, albumname)
      |);
      |
      |SELECT * FROM song JOIN <caret>
      """.trimMargin(),
    )

    myFixture.complete(CompletionType.BASIC)

    val lookupElementStrings = myFixture.lookupElementStrings
    assertThat(lookupElementStrings).contains("album ON song.songartist = album.albumartist AND song.songalbum = album.albumname")
  }
}
