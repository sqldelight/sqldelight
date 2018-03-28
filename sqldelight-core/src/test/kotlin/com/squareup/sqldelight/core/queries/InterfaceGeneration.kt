package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.QueryInterfaceGenerator
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InterfaceGeneration {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test fun noUniqueQueries() {
    checkFixtureCompiles("no-unique-queries")
  }

  @Test fun queryRequiresType() {
    checkFixtureCompiles("query-requires-type")
  }

  @Test fun `left joins apply nullability to columns`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE A(
      |  val1 TEXT NOT NULL
      |);
      |
      |CREATE TABLE B(
      |  val2 TEXT NOT NULL
      |);
      |
      |leftJoin:
      |SELECT *
      |FROM A LEFT OUTER JOIN B;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface LeftJoin {
      |    val val1: kotlin.String
      |
      |    val val2: kotlin.String?
      |
      |    data class Impl(override val val1: kotlin.String, override val val2: kotlin.String?) : com.example.LeftJoin
      |}
      |""".trimMargin())
  }

  @Test fun `duplicated column name uses table prefix`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE A(
      |  value TEXT NOT NULL
      |);
      |
      |CREATE TABLE B(
      |  value TEXT NOT NULL
      |);
      |
      |leftJoin:
      |SELECT *
      |FROM A JOIN B;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface LeftJoin {
      |    val value: kotlin.String
      |
      |    val value_: kotlin.String
      |
      |    data class Impl(override val value: kotlin.String, override val value_: kotlin.String) : com.example.LeftJoin
      |}
      |""".trimMargin())
  }

  @Test fun `incompatible adapter types revert to sqlite types`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE A(
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |CREATE TABLE B(
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |unionOfBoth:
      |SELECT value, value
      |FROM A
      |UNION
      |SELECT value, value
      |FROM B;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface UnionOfBoth {
      |    val value: kotlin.String?
      |
      |    val value_: kotlin.String?
      |
      |    data class Impl(override val value: kotlin.String?, override val value_: kotlin.String?) : com.example.UnionOfBoth
      |}
      |""".trimMargin())
  }

  @Test fun `compatible adapter types merges nullability`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE A(
      |  value TEXT AS kotlin.collections.List NOT NULL
      |);
      |
      |unionOfBoth:
      |SELECT value, value
      |FROM A
      |UNION
      |SELECT value, nullif(value, 1 == 1)
      |FROM A;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface UnionOfBoth {
      |    val value: kotlin.collections.List
      |
      |    val value_: kotlin.collections.List?
      |
      |    data class Impl(override val value: kotlin.collections.List, override val value_: kotlin.collections.List?) : com.example.UnionOfBoth
      |}
      |""".trimMargin())
  }

  @Test fun `null type uses the other column in a union`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE A(
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |unionOfBoth:
      |SELECT value, NULL
      |FROM A
      |UNION
      |SELECT NULL, value
      |FROM A;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface UnionOfBoth {
      |    val value: kotlin.collections.List?
      |
      |    val expr: kotlin.collections.List?
      |
      |    data class Impl(override val value: kotlin.collections.List?, override val expr: kotlin.collections.List?) : com.example.UnionOfBoth
      |}
      |""".trimMargin())
  }

  @Test fun `argument type uses the other column in a union`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE A(
      |  value TEXT AS kotlin.collections.List NOT NULL
      |);
      |
      |unionOfBoth:
      |SELECT value, ?
      |FROM A
      |UNION
      |SELECT value, value
      |FROM A;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface UnionOfBoth {
      |    val value: kotlin.collections.List
      |
      |    val expr: kotlin.collections.List
      |
      |    data class Impl(override val value: kotlin.collections.List, override val expr: kotlin.collections.List) : com.example.UnionOfBoth
      |}
      |""".trimMargin())
  }

  @Test fun `union with enum adapter required works fine`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE TestBModel (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  name TEXT NOT NULL,
      |  address TEXT NOT NULL
      |);
      |CREATE TABLE TestAModel (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  name TEXT NOT NULL,
      |  address TEXT NOT NULL,
      |  status TEXT as TestADbModel.Status NOT NULL
      |);
      |
      |select_all:
      |SELECT *
      |FROM TestAModel
      |JOIN TestBModel ON TestAModel.name = TestBModel.name
      |
      |UNION
      |
      |SELECT *
      |FROM TestAModel
      |JOIN TestBModel ON TestAModel.address = TestBModel.address;
    """.trimMargin(), temporaryFolder)

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinInterfaceSpec().toString()).isEqualTo("""
      |interface Select_all {
      |    val _id: kotlin.Long
      |
      |    val name: kotlin.String
      |
      |    val address: kotlin.String
      |
      |    val status: TestADbModel.Status
      |
      |    val _id_: kotlin.Long
      |
      |    val name_: kotlin.String
      |
      |    val address_: kotlin.String
      |
      |    data class Impl(
      |            override val _id: kotlin.Long,
      |            override val name: kotlin.String,
      |            override val address: kotlin.String,
      |            override val status: TestADbModel.Status,
      |            override val _id_: kotlin.Long,
      |            override val name_: kotlin.String,
      |            override val address_: kotlin.String
      |    ) : com.example.Select_all
      |}
      |""".trimMargin())
  }

  private fun checkFixtureCompiles(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture(
        "src/test/query-interface-fixtures/$fixtureRoot",
        SqlDelightCompiler::writeQueryInterfaces,
        false)
    for ((expectedFile, actualOutput) in result.compilerOutput) {
      assertThat(expectedFile.exists()).named("No file with name $expectedFile").isTrue()
      assertThat(expectedFile.readText()).named(expectedFile.name).isEqualTo(
          actualOutput.toString())
    }
  }
}
