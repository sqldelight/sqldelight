package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.asClassName
import com.squareup.sqldelight.core.compiler.SelectQueryGenerator
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExpressionTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `and has lower precedence than like`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  TestId INTEGER NOT NULL,
      |  TestText TEXT NOT NULL COLLATE NOCASE,
      |  SecondId INTEGER NOT NULL,
      |  PRIMARY KEY (TestId)
      |);
      |
      |testQuery:
      |SELECT *
      |FROM test
      |WHERE SecondId = ? AND TestText LIKE ? ESCAPE '\' COLLATE NOCASE;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    Truth.assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo("""
      |fun testQuery(SecondId: kotlin.Long, value: kotlin.String): com.squareup.sqldelight.Query<com.example.Test> = testQuery(SecondId, value, com.example.Test::Impl)
      |""".trimMargin())
  }

  @Test fun `case expression has correct type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  id INTEGER PRIMARY KEY AUTOINCREMENT,
      |  some_text TEXT NOT NULL
      |);
      |
      |someSelect:
      |SELECT CASE id WHEN 0 THEN some_text ELSE some_text + id END AS indexed_text
      |FROM test;
      """.trimMargin(), tempFolder
    )

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.single().javaType).isEqualTo(String::class.asClassName())
  }

  @Test fun `cast expression has correct type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT
      |);
      |
      |selectStuff:
      |SELECT id, CAST (id AS TEXT)
      |FROM test;
      """.trimMargin(), tempFolder
    )

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.map { it.javaType }).containsExactly(
       LONG, String::class.asClassName()
    )
  }

  @Test fun `like expression has correct type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE employee (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  department TEXT NOT NULL,
      |  name TEXT NOT NULL,
      |  title TEXT NOT NULL,
      |  bio TEXT NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM employee
      |WHERE department = ?
      |AND (
      |  name LIKE '%' || ? || '%'
      |  OR title LIKE '%' || ? || '%'
      |  OR bio LIKE '%' || ? || '%'
      |)
      |ORDER BY department;
      """.trimMargin(), tempFolder
    )

    val query = file.namedQueries.first()
    Truth.assertThat(query.arguments.map { it.type.javaType }).containsExactly(
        String::class.asClassName(), String::class.asClassName(), String::class.asClassName(),
        String::class.asClassName()
    )
  }
}