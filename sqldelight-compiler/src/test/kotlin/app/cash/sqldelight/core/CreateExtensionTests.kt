package app.cash.sqldelight.core

import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CreateExtensionTests {
  @get:Rule
  val tempFolder = TemporaryFolder()

  @Test
  fun `create extension is parsed`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
      |
      |CREATE TABLE Person(
      |id UUID PRIMARY KEY DEFAULT (uuid_generate_v4()),
      |createdAt TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      |fullName TEXT NOT NULL
      |);
      |
      |selectPerson:
      |SELECT *
      |FROM Person
      |WHERE id = ?;
      """.trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect(),
    )
  }
}
