package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.MutatorQueryGenerator
import com.squareup.sqldelight.core.compiler.SelectQueryGenerator
import com.squareup.sqldelight.core.compiler.model.namedMutators
import com.squareup.sqldelight.core.compiler.model.namedQueries
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

    val selectGenerator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo("""
      |/**
      | * Queries all values.
      | */
      |fun selectAll(): com.squareup.sqldelight.Query<com.example.Test> = selectAll(com.example.Test::Impl)
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

    val selectGenerator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo("""
      |/**
      | * Queries all values.
      | * Returns values as a List.
      | *
      | * @deprecated Don't use it!
      | */
      |fun selectAll(): com.squareup.sqldelight.Query<com.example.Test> = selectAll(com.example.Test::Impl)
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

    val selectGenerator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo("""
      |/**
      | * Queries all values. **
      | * Returns values as a * List.
      | *
      | * ** @deprecated Don't use it!
      | */
      |fun selectAll(): com.squareup.sqldelight.Query<com.example.Test> = selectAll(com.example.Test::Impl)
      |""".trimMargin())
  }

  @Test fun `select - single line javadoc`() {
    val file = FixtureCompiler.parseSql(CREATE_TABLE + """
      |/** Queries all values. */
      |selectAll:
      |SELECT *
      |FROM test;
      |""".trimMargin(), tempFolder)

    val selectGenerator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo("""
      |/**
      | * Queries all values.
      | */
      |fun selectAll(): com.squareup.sqldelight.Query<com.example.Test> = selectAll(com.example.Test::Impl)
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

    val selectGenerator = SelectQueryGenerator(file.sqliteStatements().namedQueries().first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo("""
      |/**
      | * Queries all values.
      | */
      |fun selectAll(): com.squareup.sqldelight.Query<com.example.Test> = selectAll(com.example.Test::Impl)
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

    val insertGenerator = MutatorQueryGenerator(file.sqliteStatements().namedMutators().first())
    assertThat(insertGenerator.function().toString()).isEqualTo("""
      |/**
      | * Insert new value.
      | */
      |fun insertValue(value: kotlin.String): kotlin.Long = insertValue.execute(value)
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

    val updateGenerator = MutatorQueryGenerator(file.sqliteStatements().namedMutators().first())
    assertThat(updateGenerator.function().toString()).isEqualTo("""
      |/**
      | * Update value by id.
      | */
      |fun updateById(value: kotlin.String, _id: kotlin.Long): kotlin.Long = updateById.execute(value, _id)
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

    val deleteGenerator = MutatorQueryGenerator(file.sqliteStatements().namedMutators().first())
    assertThat(deleteGenerator.function().toString()).isEqualTo("""
      |/**
      | * Delete all.
      | */
      |fun deleteAll(): kotlin.Long = deleteAll.execute()
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