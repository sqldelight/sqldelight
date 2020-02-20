package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
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
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo("""
      |override fun testQuery(SecondId: kotlin.Long, value: kotlin.String): com.squareup.sqldelight.Query<com.example.Test> = testQuery(SecondId, value, com.example.Test::Impl)
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
    assertThat(query.resultColumns.single().javaType).isEqualTo(String::class.asClassName())
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
    assertThat(query.resultColumns.map { it.javaType }).containsExactly(
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
    assertThat(query.arguments.map { it.type.javaType }).containsExactly(
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
    assertThat(query.resultColumns.map { it.javaType }).containsExactly(
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
    assertThat(query.resultColumns.single().javaType).isEqualTo(LONG.copy(nullable = true))
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
    assertThat(query.resultColumns.single().javaType).isEqualTo(DOUBLE.copy(nullable = true))
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
    assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        String::class.asClassName().copy(nullable = true), String::class.asClassName()
    ).inOrder()
  }

  @Test fun `datettime functions return non null string`() {
    val file = FixtureCompiler.parseSql("""
      |someSelect:
      |SELECT date('now');
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    assertThat(query.resultColumns.single().javaType).isEqualTo(String::class.asClassName())
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
    assertThat(query.resultColumns.single().javaType).isEqualTo(LONG)
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
    assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        LONG.copy(nullable = true), LONG.copy(nullable = true), LONG
    ).inOrder()
  }

  @Test fun `blob functions return blobs`() {
    val file = FixtureCompiler.parseSql("""
      |someSelect:
      |SELECT randomblob(), zeroblob(10);
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        ByteArray::class.asClassName(), ByteArray::class.asClassName()
    ).inOrder()
  }

  @Test fun `total function returns non null real`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value INTEGER
      |);
      |
      |someSelect:
      |SELECT total(value)
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        DOUBLE
    ).inOrder()
  }

  /**
   * avg's output is nullable because it returns NULL for an input that's empty
   * or only contains NULLs.
   *
   * https://www.sqlite.org/lang_aggfunc.html#avg:
   * >> The result of avg() is NULL if and only if there are no non-NULL inputs.
   */
  @Test fun `avg function returns nullable real`() {
    val file = FixtureCompiler.parseSql("""
    |CREATE TABLE test (
    |  value INTEGER
    |);
    |
    |someSelect:
    |SELECT avg(value)
    |FROM test;
    """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        DOUBLE.copy(nullable = true)
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
    assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        LONG.copy(nullable = true), DOUBLE
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
    assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        ByteArray::class.asClassName(),
        ByteArray::class.asClassName(),
        String::class.asClassName(),
        DOUBLE.copy(nullable = true),
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
    assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        ByteArray::class.asClassName().copy(nullable = true),
        ByteArray::class.asClassName().copy(nullable = true),
        String::class.asClassName().copy(nullable = true),
        DOUBLE.copy(nullable = true),
        LONG.copy(nullable = true)
    ).inOrder()
  }

  @Test fun `case expression part of limit infers type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value TEXT
      |);
      |
      |someSelect:
      |SELECT value
      |FROM test
      |LIMIT CASE WHEN (SELECT 1) < :arg1
      |  THEN :arg2
      |  ELSE :arg3
      |  END
      |;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    assertThat(query.parameters.size).isEqualTo(3)
    assertThat(query.parameters[0].javaType).isEqualTo(LONG)
    assertThat(query.parameters[1].javaType).isEqualTo(LONG)
    assertThat(query.parameters[2].javaType).isEqualTo(LONG)
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
    assertThat(query.resultColumns.map { it.javaType }).containsExactly(
        LONG.copy(nullable = true),
        DOUBLE.copy(nullable = true),
        DOUBLE.copy(nullable = true),
        String::class.asClassName().copy(nullable = true),
        ByteArray::class.asClassName().copy(nullable = true)
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
    assertThat(query.resultColumns.single().javaType).isEqualTo(LONG.copy(nullable = true))
  }

  @Test fun `insert expression gets the right type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value1 TEXT,
      |  value2 TEXT
      |);
      |
      |insert:
      |INSERT INTO test
      |SELECT ?, value2
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedMutators.first()
    assertThat(query.parameters.single().javaType).isEqualTo(String::class.asClassName().copy(nullable = true))
  }

  @Test fun `insert expression gets the right type from inner query`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE test (
      |  value1 TEXT,
      |  value2 TEXT
      |);
      |
      |insert:
      |INSERT INTO test
      |SELECT (SELECT ?), value2
      |FROM test;
      """.trimMargin(), tempFolder)

    val query = file.namedMutators.first()
    assertThat(query.parameters.single().javaType).isEqualTo(String::class.asClassName().copy(nullable = true))
  }
}
