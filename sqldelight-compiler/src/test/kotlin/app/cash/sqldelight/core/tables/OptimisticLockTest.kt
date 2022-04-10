package app.cash.sqldelight.core.tables

import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth
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

    Truth.assertThat(result.errors).containsExactly(
      "Test.sq: (8, 0): This query updates a table with an optimistic lock but does not correctly use the lock."
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

    Truth.assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(File(result.outputDirectory, "com/example/Test.kt"))
    Truth.assertThat(generatedInterface).isNotNull()
    Truth.assertThat(generatedInterface.toString()).isEqualTo(
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
}
