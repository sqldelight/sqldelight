package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.DOUBLE
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
      """.trimMargin(), tempFolder)

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
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.map { it.javaType }).containsExactly(
       LONG, String::class.asClassName()
    ).inOrder()
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
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.arguments.map { it.type.javaType }).containsExactly(
        String::class.asClassName(), String::class.asClassName(), String::class.asClassName(),
        String::class.asClassName()
    ).inOrder()
  }

  @Test fun `round function has correct type`() {
    val file = FixtureCompiler.parseSql("""
      |someSelect:
      |SELECT round(1.123123), round(1.12312, 3);
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        LONG, DOUBLE
    ).inOrder()
  }

  @Test fun `sum function has right type for all non-null ints`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value INTEGER NOT NULL
      |);
      |
      |someSelect:
      |SELECT sum(value)
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.single().javaType).isEqualTo(LONG)
  }

  @Test fun `sum function has right type for nullable values`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value INTEGER
      |);
      |
      |someSelect:
      |SELECT sum(value)
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.single().javaType).isEqualTo(DOUBLE.asNullable())
  }

  @Test fun `string functions return nullable string only if parameter is nullable`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value1 TEXT,
      |  value2 TEXT NOT NULL
      |);
      |
      |someSelect:
      |SELECT lower(value1), lower(value2)
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        String::class.asClassName().asNullable(), String::class.asClassName()
    ).inOrder()
  }

  @Test fun `datettime functions return non null string`() {
    val file = FixtureCompiler.parseSql("""
      |someSelect:
      |SELECT date('now');
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.single().javaType).isEqualTo(String::class.asClassName())
  }

  @Test fun `count function returns non null integer`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value1 TEXT
      |);
      |
      |someSelect:
      |SELECT count(*)
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.single().javaType).isEqualTo(LONG)
  }

  @Test fun `instr function returns nullable int if any of the args are null`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value1 TEXT,
      |  value2 TEXT NOT NULL
      |);
      |
      |someSelect:
      |SELECT instr(value1, value2), instr(value2, value1), instr(value2, value2)
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        LONG.asNullable(), LONG.asNullable(), LONG
    ).inOrder()
  }

  @Test fun `blob functions return blobs`() {
    val file = FixtureCompiler.parseSql("""
      |someSelect:
      |SELECT randomblob(), zeroblob(10);
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        ByteArray::class.asClassName(), ByteArray::class.asClassName()
    ).inOrder()
  }

  @Test fun `aggregate real functions return non null reals`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value INTEGER
      |);
      |
      |someSelect:
      |SELECT total(value), avg(value)
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        DOUBLE, DOUBLE
    ).inOrder()
  }

  @Test fun `abs function returns the same type as given`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value INTEGER,
      |  value2 REAL NOT NULL
      |);
      |
      |someSelect:
      |SELECT abs(value), abs(value2)
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        LONG.asNullable(), DOUBLE
    ).inOrder()
  }

  @Test fun `coalesce takes the proper type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  integerVal INTEGER NOT NULL,
      |  realVal REAL NOT NULL,
      |  nullableRealVal REAL,
      |  textVal TEXT,
      |  blobVal BLOB
      |);
      |
      |someSelect:
      |SELECT coalesce(integerVal, realVal, textVal, blobVal),
      |       coalesce(integerVal, nullableRealVal, textVal, blobVal),
      |       coalesce(integerVal, nullableRealVal, textVal),
      |       coalesce(nullableRealVal),
      |       coalesce(integerVal)
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        ByteArray::class.asClassName(),
        ByteArray::class.asClassName(),
        String::class.asClassName(),
        DOUBLE.asNullable(),
        LONG
    ).inOrder()
  }

  @Test fun `max takes the proper type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  integerVal INTEGER NOT NULL,
      |  realVal REAL NOT NULL,
      |  nullableRealVal REAL,
      |  textVal TEXT,
      |  blobVal BLOB
      |);
      |
      |someSelect:
      |SELECT max(integerVal, realVal, textVal, blobVal),
      |       max(integerVal, nullableRealVal, textVal, blobVal),
      |       max(integerVal, nullableRealVal, textVal),
      |       max(nullableRealVal),
      |       max(integerVal)
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        ByteArray::class.asClassName().asNullable(),
        ByteArray::class.asClassName().asNullable(),
        String::class.asClassName().asNullable(),
        DOUBLE.asNullable(),
        LONG.asNullable()
    ).inOrder()
  }

  @Test fun `min takes the proper type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  integerVal INTEGER NOT NULL,
      |  realVal REAL NOT NULL,
      |  nullableRealVal REAL,
      |  textVal TEXT NOT NULL,
      |  blobVal BLOB NOT NULL
      |);
      |
      |someSelect:
      |SELECT min(integerVal, textVal, blobVal),
      |       min(integerVal, nullableRealVal, textVal, blobVal),
      |       min(nullableRealVal),
      |       min(blobVal, textVal),
      |       min(blobVal)
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        LONG.asNullable(),
        DOUBLE.asNullable(),
        DOUBLE.asNullable(),
        String::class.asClassName().asNullable(),
        ByteArray::class.asClassName().asNullable()
    ).inOrder()
  }

  @Test fun `nullif take nullable version of given type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value1 INTEGER NOT NULL,
      |  value2 REAL NOT NULL
      |);
      |
      |someSelect:
      |SELECT nullif(value1, value2)
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.single().javaType).isEqualTo(LONG.asNullable())
  }

  @Test fun `binary expression gets the right type`() {
    val file = FixtureCompiler.parseSql("""
      |someSelect:
      |SELECT 10 > 12;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    Truth.assertThat(query.resultColumns.single().javaType).isEqualTo(BOOLEAN)
  }


}