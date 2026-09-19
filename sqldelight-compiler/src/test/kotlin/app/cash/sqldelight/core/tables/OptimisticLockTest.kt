package app.cash.sqldelight.core.tables

import app.cash.sqldelight.core.TestDialect
import app.cash.sqldelight.core.compiler.MutatorQueryGenerator
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.withUnderscores
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OptimisticLockTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `updates with no optimistic lock fail`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |updateText:
      |UPDATE test
      |SET text = :text
      |WHERE id = :id;
      |
      """.trimMargin(),
      tempFolder,
    )

    assertThat(result.errors).containsExactly(
      "Test.sq: (8, 0): This statement is missing the optimistic lock in its SET clause.",
    )
  }

  @Test fun `multi row updates with self increment are allowed`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |updateText:
      |UPDATE test
      |SET text = :text
      |, version = version + 1;
      |
      """.trimMargin(),
      tempFolder,
    )

    assertThat(result.errors).isEmpty()
  }

  @Test fun `optimistic lock generates a value type`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS LOCK NOT NULL
      |);
      |
      """.trimMargin(),
      tempFolder,
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(File(result.outputDirectory, "com/example/Test.kt"))
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |@file:Suppress("REDUNDANT_VISIBILITY_MODIFIER", "ASSIGNED_VALUE_IS_NEVER_READ")
      |
      |package com.example
      |
      |import kotlin.Long
      |import kotlin.jvm.JvmInline
      |
      |public data class Test(
      |  public val id: Id,
      |) {
      |  @JvmInline
      |  public value class Id(
      |    public val id: Long,
      |  )
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `upsert missing optimistic lock in DO UPDATE fails`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE
      |SET text = excluded.text
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_24.dialect,
    )

    assertThat(result.errors).containsExactly(
      "Test.sq: (11, 0): This statement is missing the optimistic lock in its SET clause.",
    )
  }

  @Test fun `upsert with correct optimistic lock is allowed`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE SET
      |  text = excluded.text,
      |  version = :version + 1
      |WHERE version = :version
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_24.dialect,
    )

    assertThat(result.errors).isEmpty()
  }

  @Test fun `upsert with a self incrementing optimistic lock is allowed`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE SET
      |  text = excluded.text,
      |  version = version + 1
      |WHERE version = :version
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_24.dialect,
    )

    assertThat(result.errors).isEmpty()
  }

  @Test fun `upsert self incrementing the optimistic lock without a WHERE clause fails`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE SET
      |  text = excluded.text,
      |  version = version + 1
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_24.dialect,
    )

    assertThat(result.errors).containsExactly(
      "Test.sq: (10, 27): This statement is missing a WHERE clause to check the optimistic lock.",
    )
  }

  @Test fun `upsert self incrementing the optimistic lock without checking it fails`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE SET
      |  text = excluded.text,
      |  version = version + 1
      |WHERE id = :id
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_24.dialect,
    )

    assertThat(result.errors).containsExactly(
      """Test.sq: (10, 27): The optimistic lock must be queried exactly like "version == :version".""",
    )
  }

  @Test fun `upsert incrementing the excluded optimistic lock fails`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE
      |SET version = excluded.version + 1
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_24.dialect,
    )

    assertThat(result.errors).containsExactly(
      """Test.sq: (11, 0): The optimistic lock must be set exactly like "version = :version + 1".""",
    )
  }

  @Test fun `upsert self incrementing an aliased optimistic lock is allowed`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test AS t (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE SET
      |  text = excluded.text,
      |  version = t.version + 1
      |WHERE t.version = :version
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_24.dialect,
    )

    assertThat(result.errors).isEmpty()
  }

  @Test fun `upsert checking the excluded optimistic lock fails`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE
      |SET version = :version + 1
      |WHERE excluded.version = :version
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_24.dialect,
    )

    assertThat(result.errors).containsExactly(
      """Test.sq: (11, 0): The optimistic lock must be queried exactly like "version == :version".""",
    )
  }

  @Test fun `upsert with an optimistic lock that is not incremented fails`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE
      |SET version = :version + 2
      |WHERE version = :version
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_24.dialect,
    )

    assertThat(result.errors).containsExactly(
      """Test.sq: (11, 0): The optimistic lock must be set exactly like "version = :version + 1".""",
    )
  }

  @Test fun `upsert missing optimistic lock in DO UPDATE fails on sqlite 3_35`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE
      |SET text = excluded.text
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_35.dialect,
    )

    assertThat(result.errors).containsExactly(
      "Test.sq: (11, 0): This statement is missing the optimistic lock in its SET clause.",
    )
  }

  @Test fun `upsert incrementing the excluded optimistic lock fails on sqlite 3_35`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE
      |SET version = excluded.version + 1
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_35.dialect,
    )

    assertThat(result.errors).containsExactly(
      """Test.sq: (11, 0): The optimistic lock must be set exactly like "version = :version + 1".""",
    )
  }

  @Test fun `upsert self incrementing a quoted optimistic lock is allowed`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE SET
      |  text = excluded.text,
      |  version = [test].version + 1
      |WHERE version = :version
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_24.dialect,
    )

    assertThat(result.errors).isEmpty()
  }

  @Test fun `upsert checks every conflict clause on sqlite 3_35`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  other INTEGER NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, other, text, version)
      |VALUES (?, ?, ?, ?)
      |ON CONFLICT (id) DO UPDATE
      |SET text = excluded.text
      |ON CONFLICT (other) DO UPDATE
      |SET text = excluded.text
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_35.dialect,
    )

    assertThat(result.errors).containsExactly(
      "Test.sq: (12, 0): This statement is missing the optimistic lock in its SET clause.",
      "Test.sq: (14, 0): This statement is missing the optimistic lock in its SET clause.",
    )
  }

  @Test fun `upsert with a lock check on every conflict clause is allowed on sqlite 3_35`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  other INTEGER NOT NULL,
      |  version INTEGER AS LOCK NOT NULL DEFAULT 0,
      |  text TEXT NOT NULL,
      |  UNIQUE (id),
      |  UNIQUE (other)
      |);
      |
      |upsertText:
      |INSERT INTO test (id, other, text)
      |VALUES (:id, :other, :text)
      |ON CONFLICT (id) DO UPDATE
      |SET text = :text, version = version + 1
      |WHERE version = :version
      |ON CONFLICT (other) DO UPDATE
      |SET text = :text, version = version + 1
      |WHERE version = :version
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_35.dialect,
    )

    assertThat(result.errors).isEmpty()
  }

  @Test fun `upsert self incrementing a quoted optimistic lock is allowed on sqlite 3_35`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE SET
      |  text = excluded.text,
      |  version = [test].version + 1
      |WHERE version = :version
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_35.dialect,
    )

    assertThat(result.errors).isEmpty()
  }

  @Test fun `upsert self incrementing the optimistic lock without a WHERE clause fails on sqlite 3_35`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE SET
      |  text = excluded.text,
      |  version = version + 1
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_35.dialect,
    )

    assertThat(result.errors).containsExactly(
      "Test.sq: (10, 27): This statement is missing a WHERE clause to check the optimistic lock.",
    )
  }

  @Test fun `upsert self incrementing the optimistic lock without checking it fails on sqlite 3_35`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL,
      |  text TEXT NOT NULL
      |);
      |
      |upsertText:
      |INSERT INTO test (id, text, version)
      |VALUES (?, ?, ?)
      |ON CONFLICT (id) DO UPDATE SET
      |  text = excluded.text,
      |  version = version + 1
      |WHERE id = :id
      |;
      |
      """.trimMargin(),
      tempFolder,
      overrideDialect = TestDialect.SQLITE_3_35.dialect,
    )

    assertThat(result.errors).containsExactly(
      """Test.sq: (10, 27): The optimistic lock must be queried exactly like "version == :version".""",
    )
  }

  @Test fun `update query correctly generates`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE test (
      |  id SERIAL AS VALUE NOT NULL,
      |  version INTEGER AS LOCK NOT NULL DEFAULT 0,
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
      |
      """.trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect(),
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)
    assertThat(generator.function().toString()).isEqualTo(
      """
      |/**
      | * @return The number of rows updated.
      | */
      |public fun updateText(
      |  text: kotlin.String,
      |  version: com.example.Test.Version,
      |  id: com.example.Test.Id,
      |): app.cash.sqldelight.db.QueryResult<kotlin.Long> {
      |  val result = driver.execute(${mutator.id.withUnderscores}, ""${'"'}
      |      |UPDATE test
      |      |SET
      |      |  text = ?,
      |      |  version = ? + 1
      |      |WHERE
      |      |  id = ? AND
      |      |  version = ?
      |      ""${'"'}.trimMargin(), 4) {
      |        check(this is app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement)
      |        var parameterIndex = 0
      |        bindString(parameterIndex++, text)
      |        bindInt(parameterIndex++, version.version)
      |        bindInt(parameterIndex++, id.id)
      |        bindInt(parameterIndex++, version.version)
      |      }
      |  if (result.value == 0L) throw app.cash.sqldelight.db.OptimisticLockException("UPDATE on test failed because optimistic lock version did not match")
      |  notifyQueries(${mutator.id.withUnderscores}) { emit ->
      |    emit("test")
      |  }
      |  return result
      |}
      |
      """.trimMargin(),
    )
  }
}
