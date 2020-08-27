/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.sqldelight.intellij.lang

import com.intellij.codeInsight.generation.actions.CommentByLineCommentAction
import com.intellij.openapi.actionSystem.IdeActions
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.intellij.SqlDelightFixtureTestCase

class SqlDelightCommenterTest : SqlDelightFixtureTestCase() {

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
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)

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
