package app.cash.sqldelight.intellij.annotations

import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.intellij.SqlDelightProjectTestCase

class OptimisticLockAnnotatorTest : SqlDelightProjectTestCase() {
  fun testWorkingLock() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |UPDATE test
        |SET
        |  text = :text,
        |  version = :version + 1
        |WHERE
        |  id = :id AND
        |  version = :version
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testNoLockSpecified() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |<error descr="This statement is missing the optimistic lock in it's SET clause.">UPDATE test
        |SET
        |  text = :text
        |WHERE
        |  id = :id</error>
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testNoLockSetter() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |<error descr="This statement is missing the optimistic lock in it's SET clause.">UPDATE test
        |SET
        |  text = :text
        |WHERE
        |  id = :id AND
        |  version = :version</error>
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testNoLockQuery() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |<error descr="The optimistic lock must be queried exactly like \"version == :version\".">UPDATE test
        |SET
        |  text = :text,
        |  version = :version + 1
        |WHERE
        |  id = :id</error>
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testLockSetIncorrectly() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |<error descr="The optimistic lock must be set exactly like \"version = :version + 1\".">UPDATE test
        |SET
        |  text = :text,
        |  version = :version + 2
        |WHERE
        |  id = :id AND
        |  version = :version</error>
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testLockSetIncorrectlyWithMinus() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |<error descr="The optimistic lock must be set exactly like \"version = :version + 1\".">UPDATE test
        |SET
        |  text = :text,
        |  version = :version - 1
        |WHERE
        |  id = :id AND
        |  version = :version</error>
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testLockCheckedIncorrectly() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |<error descr="The optimistic lock must be queried exactly like \"version == :version\".">UPDATE test
        |SET
        |  text = :text,
        |  version = :version + 1
        |WHERE
        |  id = :id AND
        |  version <> :version</error>
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testWorkingLockLimit() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |UPDATE test
        |SET
        |  text = :text,
        |  version = :version + 1
        |WHERE
        |  id = :id AND
        |  version = :version
        |LIMIT 10
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testNoLockSpecifiedLimit() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |<error descr="This statement is missing the optimistic lock in it's SET clause.">UPDATE test
        |SET
        |  text = :text
        |WHERE
        |  id = :id
        |LIMIT 10</error>
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testNoLockSetterLimit() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |<error descr="This statement is missing the optimistic lock in it's SET clause.">UPDATE test
        |SET
        |  text = :text
        |WHERE
        |  id = :id AND
        |  version = :version
        |LIMIT 10</error>
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testNoLockQueryLimit() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |<error descr="The optimistic lock must be queried exactly like \"version == :version\".">UPDATE test
        |SET
        |  text = :text,
        |  version = :version + 1
        |WHERE
        |  id = :id
        |LIMIT 10</error>
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testLockSetIncorrectlyLimit() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |<error descr="The optimistic lock must be set exactly like \"version = :version + 1\".">UPDATE test
        |SET
        |  text = :text,
        |  version = :version + 2
        |WHERE
        |  id = :id AND
        |  version = :version
        |LIMIT 10</error>
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testLockSetIncorrectlyWithMinusLimit() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |<error descr="The optimistic lock must be set exactly like \"version = :version + 1\".">UPDATE test
        |SET
        |  text = :text,
        |  version = :version - 1
        |WHERE
        |  id = :id AND
        |  version = :version
        |LIMIT 10</error>
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }

  fun testLockCheckedIncorrectlyLimit() {
    myFixture.configureByText(
      SqlDelightFileType,
      """
        |CREATE TABLE test(
        |  id TEXT AS VALUE NOT NULL,
        |  version INTEGER AS LOCK NOT NULL,
        |  text TEXT NOT NULL
        |);
        |
        |updateText:
        |<error descr="The optimistic lock must be queried exactly like \"version == :version\".">UPDATE test
        |SET
        |  text = :text,
        |  version = :version + 1
        |WHERE
        |  id = :id AND
        |  version <> :version
        |LIMIT 10</error>
        |;
      """.trimMargin()
    )

    myFixture.checkHighlighting()
  }
}
