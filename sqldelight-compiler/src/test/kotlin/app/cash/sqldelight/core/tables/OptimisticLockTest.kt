package app.cash.sqldelight.core.tables

import app.cash.sqldelight.core.compiler.MutatorQueryGenerator
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

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
      |""".trimMargin(),
      tempFolder
    )

    assertThat(result.errors).containsExactly(
      "Test.sq: (8, 0): This statement is missing the optimistic lock in its SET clause."
    )
  }

  @Test fun `optimistic lock generates a value type`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  id INTEGER AS LOCK NOT NULL
      |);
      |""".trimMargin(),
      tempFolder
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(File(result.outputDirectory, "com/example/Test.kt"))
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
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
      |""".trimMargin()
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
      |""".trimMargin(),
      tempFolder, dialect = PostgreSqlDialect()
    )

    val mutator = file.namedMutators.first()
    val generator = MutatorQueryGenerator(mutator)
    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun updateText(
      |  text: kotlin.String,
      |  version: com.example.Test.Version,
      |  id: com.example.Test.Id,
      |): kotlin.Unit {
      |  val result = driver.execute(${mutator.id}, ""${'"'}
      |      |UPDATE test
      |      |SET
      |      |  text = ?,
      |      |  version = ? + 1
      |      |WHERE
      |      |  id = ? AND
      |      |  version = ?
      |      ""${'"'}.trimMargin(), 4) {
      |        check(this is app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement)
      |        bindString(1, text)
      |        bindLong(2, version.version.toLong())
      |        bindLong(3, id.id.toLong())
      |        bindLong(4, version.version.toLong())
      |      }
      |  if (result == 0L) throw app.cash.sqldelight.db.OptimisticLockException("UPDATE on test failed because optimistic lock version did not match")
      |  notifyQueries(${mutator.id}) { emit ->
      |    emit("test")
      |  }
      |}
      |""".trimMargin()
    )
  }
}
