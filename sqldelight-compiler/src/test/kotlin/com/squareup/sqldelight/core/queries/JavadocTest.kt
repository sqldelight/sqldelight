package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.MutatorQueryGenerator
import com.squareup.sqldelight.core.compiler.SelectQueryGenerator
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JavadocTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `select - properly formatted javadoc`() {
    val file = FixtureCompiler.parseSql(CREATE_TABLE + """
      |/**
      | * Queries all values.
      | */
      |selectAll:
      |SELECT *
      |FROM test;
      |""".trimMargin(), tempFolder)

    val selectGenerator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo("""
      |/**
      | * Queries all values.
      | */
      |override fun selectAll(): com.squareup.sqldelight.Query<com.example.Test> = selectAll(::com.example.Test)
      |""".trimMargin())
  }

  @Test fun `select - multiline javadoc`() {
    val file = FixtureCompiler.parseSql(CREATE_TABLE + """
      |/**
      | * Queries all values.
      | * Returns values as a List.
      | *
      | * @deprecated Don't use it!
      | */
      |selectAll:
      |SELECT *
      |FROM test;
      |""".trimMargin(), tempFolder)

    val selectGenerator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo("""
      |/**
      | * Queries all values.
      | * Returns values as a List.
      | *
      | * @deprecated Don't use it!
      | */
      |override fun selectAll(): com.squareup.sqldelight.Query<com.example.Test> = selectAll(::com.example.Test)
      |""".trimMargin())
  }

  @Test fun `select - javadoc containing * symbols`() {
    val file = FixtureCompiler.parseSql(CREATE_TABLE + """
      |/**
      | * Queries all values. **
      | * Returns values as a * List.
      | *
      | * ** @deprecated Don't use it!
      | */
      |selectAll:
      |SELECT *
      |FROM test;
      |""".trimMargin(), tempFolder)

    val selectGenerator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo("""
      |/**
      | * Queries all values. **
      | * Returns values as a * List.
      | *
      | * ** @deprecated Don't use it!
      | */
      |override fun selectAll(): com.squareup.sqldelight.Query<com.example.Test> = selectAll(::com.example.Test)
      |""".trimMargin())
  }

  @Test fun `select - single line javadoc`() {
    val file = FixtureCompiler.parseSql(CREATE_TABLE + """
      |/** Queries all values. */
      |selectAll:
      |SELECT *
      |FROM test;
      |""".trimMargin(), tempFolder)

    val selectGenerator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo("""
      |/**
      | * Queries all values.
      | */
      |override fun selectAll(): com.squareup.sqldelight.Query<com.example.Test> = selectAll(::com.example.Test)
      |""".trimMargin())
  }

  @Test fun `select - misformatted javadoc`() {
    val file = FixtureCompiler.parseSql(CREATE_TABLE + """
      |/**
      |Queries all values.
      | */
      |selectAll:
      |SELECT *
      |FROM test;
      |""".trimMargin(), tempFolder)

    val selectGenerator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo("""
      |/**
      | * Queries all values.
      | */
      |override fun selectAll(): com.squareup.sqldelight.Query<com.example.Test> = selectAll(::com.example.Test)
      |""".trimMargin())
  }

  @Test fun `insert`() {
    val file = FixtureCompiler.parseSql(CREATE_TABLE + """
      |/**
      | * Insert new value.
      | */
      |insertValue:
      |INSERT INTO test(value)
      |VALUES (?);
      |""".trimMargin(), tempFolder)

    val insert = file.namedMutators.first()
    val insertGenerator = MutatorQueryGenerator(insert)

    assertThat(insertGenerator.function().toString()).isEqualTo("""
      |/**
      | * Insert new value.
      | */
      |override fun insertValue(value: kotlin.String) {
      |  driver.execute(${insert.id}, ""${'"'}
      |  |INSERT INTO test(value)
      |  |VALUES (?)
      |  ""${'"'}.trimMargin(), 1) {
      |    bindString(1, value)
      |  }
      |}
      |""".trimMargin())
  }

  @Test fun `update`() {
    val file = FixtureCompiler.parseSql(CREATE_TABLE + """
      |/**
      | * Update value by id.
      | */
      |updateById:
      |UPDATE test
      |SET value = ?
      |WHERE _id = ?;
      |""".trimMargin(), tempFolder)

    val update = file.namedMutators.first()
    val updateGenerator = MutatorQueryGenerator(update)

    assertThat(updateGenerator.function().toString()).isEqualTo("""
      |/**
      | * Update value by id.
      | */
      |override fun updateById(value: kotlin.String, _id: kotlin.Long) {
      |  driver.execute(${update.id}, ""${'"'}
      |  |UPDATE test
      |  |SET value = ?
      |  |WHERE _id = ?
      |  ""${'"'}.trimMargin(), 2) {
      |    bindString(1, value)
      |    bindLong(2, _id)
      |  }
      |}
      |""".trimMargin())
  }

  @Test fun `delete`() {
    val file = FixtureCompiler.parseSql(CREATE_TABLE + """
      |/**
      | * Delete all.
      | */
      |deleteAll:
      |DELETE FROM test;
      |""".trimMargin(), tempFolder)

    val delete = file.namedMutators.first()
    val deleteGenerator = MutatorQueryGenerator(delete)

    assertThat(deleteGenerator.function().toString()).isEqualTo("""
      |/**
      | * Delete all.
      | */
      |override fun deleteAll() {
      |  driver.execute(${delete.id}, ""${'"'}DELETE FROM test""${'"'}, 0)
      |}
      |""".trimMargin())
  }

  companion object {
    private val CREATE_TABLE = """
      |CREATE TABLE test (
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  value TEXT NOT NULL
      |);
      |""".trimMargin()
  }
}
