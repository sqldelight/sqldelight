package com.squareup.sqldelight.intellij.lang

import com.intellij.codeInsight.generation.actions.CommentByLineCommentAction
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.squareup.sqldelight.core.lang.SqlDelightFileType

class SqlDelightCommenterTest : LightPlatformCodeInsightFixtureTestCase() {

  fun testSingleLineComment() {
    myFixture.configureByText(SqlDelightFileType, "<caret>SELECT *")

    val commentAction = CommentByLineCommentAction()
    commentAction.actionPerformedImpl(project, myFixture.editor)
    myFixture.checkResult("-- SELECT *")

    commentAction.actionPerformedImpl(project, myFixture.editor)
    myFixture.checkResult("SELECT *")
  }

  fun testSingleLineComment_caretInTheMiddle() {
    myFixture.configureByText(SqlDelightFileType, "SEL<caret>ECT *")
    val commentAction = CommentByLineCommentAction()

    commentAction.actionPerformedImpl(project, myFixture.editor)
    myFixture.checkResult("-- SELECT *")

    commentAction.actionPerformedImpl(project, myFixture.editor)
    myFixture.checkResult("SELECT *")
  }

  fun testSingleLineComment_MultipleLines() {
    myFixture.configureByText(SqlDelightFileType, """
      |<selection>CREATE TABLE test (
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  title TEXT NOT NULL
      |);</selection>
      """.trimMargin())

    val commentAction = CommentByLineCommentAction()
    commentAction.actionPerformedImpl(project, myFixture.editor)
    myFixture.checkResult("""
      |-- CREATE TABLE test (
      |--   _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |--   title TEXT NOT NULL
      |-- );
      """.trimMargin())

    commentAction.actionPerformedImpl(project, myFixture.editor)
    myFixture.checkResult("""
      |CREATE TABLE test (
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  title TEXT NOT NULL
      |);
      """.trimMargin())
  }

  fun _testJavadoc() {
    myFixture.configureByText(SqlDelightFileType, """
      |CREATE TABLE test (
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  title TEXT NOT NULL
      |);
      |
      |<caret>
      |select_all:
      |SELECT *
      |FROM test;
      """.trimMargin())

    myFixture.type("/**")
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER);

    myFixture.checkResult("""
      |CREATE TABLE test (
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  title TEXT NOT NULL
      |);
      |
      |/**
      | * <caret>
      | */
      |select_all:
      |SELECT *
      |FROM test;
      """.trimMargin())
  }
}
