package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.MutatorQueryGenerator
import com.squareup.sqldelight.core.compiler.model.namedMutators
import com.squareup.sqldelight.core.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MutatorQueryFunctionTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `mutator method generates proper method signature`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder)

    val generator = MutatorQueryGenerator(file.sqliteStatements().namedMutators().first())

    assertThat(generator.function().toString()).isEqualTo("""
      |fun insertData(id: kotlin.Long, value: kotlin.collections.List?): kotlin.Long = insertData.execute(id, value)
      |""".trimMargin())
  }

  @Test fun `mutator method generates proper private value`() {
     val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      """.trimMargin(), tempFolder)

    val generator = MutatorQueryGenerator(file.sqliteStatements().namedMutators().first())

    assertThat(generator.value().toString()).isEqualTo("""
      |private val insertData: InsertData by lazy {
      |            InsertData(database.getConnection().prepareStatement(""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?, ?)
      |            ""${'"'}.trimMargin()))
      |        }
      |""".trimMargin())
  }
}