package app.cash.sqldelight.tests

import app.cash.sqldelight.withCommonConfiguration
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test

class MigrationTest {
  @Test fun `verification enabled fails when database file is missing`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/missing-database-file-verification-enabled"))
      .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
      .buildAndFail()

    assertThat(output.output).contains(
      "Verifying a migration requires a database file to be present. To generate one, use the generate schema Gradle task for your database. A quick way to find the task name(s) is to run `gradle :module:tasks | grep generate`.",
    )
  }

  @Test fun `verification disabled succeeds when database file is missing`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/missing-database-file-verification-disabled"))
      .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
      .build()

    assertThat(output.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun `verification disabled succeeds with hsql dialect`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/verify-migration-disabled-no-errors"))
      .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
      .build()

    assertThat(output.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun `driver initializer is executed`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-driver-initializer"))
      .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
      .withDebug(true)
      .build()

    assertThat(output.output).contains("DriverInitializerImpl executed!")
    assertThat(output.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun `failing migration errors properly`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-failure"))
      .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
      .buildAndFail()

    assertThat(output.output).contains(
      """
      |Error migrating from 1.db, fresh database looks different from migration database:
      |/tables[testView] - ADDED
      |/tables[test]/columns[test."value"]/ordinalPosition - CHANGED
      |  BEFORE:
      |    2
      |  AFTER:
      |    1
      |/tables[test]/columns[test."value"]/partOfIndex - ADDED
      |/tables[test]/columns[test.value2]/attributes{IS_NULLABLE} - CHANGED
      |  BEFORE:
      |    NO
      |  AFTER:
      |    YES
      |/tables[test]/columns[test.value2]/nullable - REMOVED
      |/tables[test]/columns[test.value2]/ordinalPosition - CHANGED
      |  BEFORE:
      |    1
      |  AFTER:
      |    2
      |/tables[test]/definition - CHANGED
      |  BEFORE:
      |    CREATE TABLE test (
      |      value2 TEXT NOT NULL,
      |      value TEXT NOT NULL
      |    )
      |  AFTER:
      |    CREATE TABLE test (
      |      value TEXT NOT NULL
      |    , value2 TEXT)
      |/tables[test]/indexes[test.testIndex] - ADDED
      |/tables[test]/triggers[test.testTrigger] - ADDED
      |
      """.trimMargin(),
    )
  }

  @Test fun `schema definition changes fail migration properly`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-definition-failure"))
      .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
      .buildAndFail()

    assertThat(output.output).contains(
      """
      |Error migrating from 1.db, fresh database looks different from migration database:
      |/tables[test]/columns[test."value"]/partOfIndex - REMOVED
      |/tables[test]/columns[test.value2]/partOfIndex - ADDED
      |/tables[test]/indexes[test.testIndex]/columns[test.value2] - ADDED
      |/tables[test]/indexes[test.testIndex]/columns[test."value"] - REMOVED
      |/tables[test]/triggers[test.testTrigger]/actionStatement - CHANGED
      |  BEFORE:
      |    CREATE TRIGGER testTrigger
      |    AFTER DELETE ON test
      |    BEGIN
      |    INSERT INTO test VALUES ("3", "4");
      |    END
      |  AFTER:
      |    CREATE TRIGGER testTrigger
      |    AFTER DELETE ON test
      |    BEGIN
      |    INSERT INTO test VALUES ("1", "2");
      |    END
      |/tables[testView]/definition - CHANGED
      |  BEFORE:
      |    CREATE VIEW testView AS
      |    SELECT *
      |    FROM test
      |    WHERE value = 'sup'
      |  AFTER:
      |    CREATE VIEW testView AS
      |    SELECT *
      |    FROM test
      |
      """.trimMargin(),
    )
  }

  @Test fun `migration file with errors reports file errors`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-syntax-failure"))
      .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
      .buildAndFail()

    assertThat(output.output).contains(
      """
      |1.sqm:1:5 TABLE expected, got 'TABE'
      |1    ALTER TABE test ADD COLUMN value2 TEXT
      """.trimMargin(),
    )
  }

  @Test fun `deriving schema from migration introduces failures in migration files`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-schema-failure"))
      .withArguments("clean", "build", "--stacktrace")
      .buildAndFail()

    assertThat(output.output).contains(
      """
      |1.sqm:5:22 No column found with name new_column
      |5    INSERT INTO test (id, new_column)
      |                           ^^^^^^^^^^
      |6    VALUES ("hello", "world")
      """.trimMargin(),
    )
  }

  @Test fun `successful migration works properly`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-success"))
      .withArguments("clean", "check", "verifyMainDatabaseMigration", "--stacktrace")
      .build()

    assertThat(output.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun `successful migration when folder contains db extension`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-success-db-directory-name"))
      .withArguments("clean", "check", "verifyMainDatabaseMigration", "--stacktrace")
      .build()

    assertThat(output.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun `multiple databases can have separate migrations`() {
    val fixtureRoot = File("src/test/multiple-project-migration-success")

    var output = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "check", "verifyMainDatabaseAMigration", "--stacktrace")
      .build()

    assertThat(output.output).contains("BUILD SUCCESSFUL")

    output = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "check", "verifyMainDatabaseBMigration", "--stacktrace")
      .build()

    assertThat(output.output).contains("BUILD SUCCESSFUL")
  }

  @Test fun `don't print java-object-diff warnings on default log level`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-success"))
      .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
      .build()

    assertThat(output.output).doesNotContain("Detected circular reference in node at path")
  }

  @Test fun `print java-object-diff warnings on debug log level`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-success"))
      .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace", "--debug")
      .build()

    assertThat(output.output).contains("""Detected circular reference in node at path /tables[test]/indexes[test.testIndex]/columns[test."value"]/index Going deeper would cause an infinite loop, so I'll stop looking at this instance along the current path.""")
  }

  @Test fun `migrations with a gap errors properly`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-gap-failure"))
      .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
      .buildAndFail()

    assertThat(output.output).contains("""Gap in migrations detected. Expected migration 2, got 3.""")
  }

  @Test fun `migrations with bad name errors properly`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-failure-name"))
      .withArguments("clean", "verifyMainDatabaseMigration", "--stacktrace")
      .buildAndFail()

    assertThat(output.output).contains("Migration files must have an integer value somewhere in their filename but nope.sqm does not.")
  }

  @Test fun `compilation fails when verifyMigrations is set to true but the migrations are incomplete`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/migration-incomplete"))
      .withArguments("clean", "generateSqlDelightInterface", "--stacktrace", "--debug")
      .buildAndFail()

    assertThat(output.output).contains("1.sqm:1:12 No table found with name test")
  }

  @Test fun `compilation succeeds when verifyMigrations is set to false but the migrations are incomplete`() {
    val fixtureRoot = File("src/test/migration-incomplete-verification-disabled")

    val output = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "generateSqlDelightInterface", "--stacktrace", "--debug")
      .build()

    assertThat(output.output).contains("BUILD SUCCESSFUL")

    val generatedDatabase = File(fixtureRoot, "build/generated/sqldelight/code/Database/main/com/example/sqldelightmigrations/DatabaseImpl.kt")
    assertThat(generatedDatabase.exists()).isTrue()
    assertThat(generatedDatabase.readText()).contains(
      """
      |private class DatabaseImpl(
      |  driver: SqlDriver,
      |) : TransacterImpl(driver),
      |    Database {
      |  override val testQueries: TestQueries = TestQueries(driver)
      |
      |  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
      |    override val version: Long
      |      get() = 2
      |
      |    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      |      driver.execute(null, ""${'"'}
      |          |CREATE TABLE test (
      |          |  value TEXT NOT NULL,
      |          |  value2 TEXT
      |          |)
      |          ""${'"'}.trimMargin(), 0)
      |      driver.execute(null, ""${'"'}
      |          |CREATE VIEW testView AS
      |          |SELECT *
      |          |FROM test
      |          ""${'"'}.trimMargin(), 0)
      |      driver.execute(null, "CREATE INDEX testIndex ON test(value)", 0)
      |      driver.execute(null, ""${'"'}
      |          |CREATE TRIGGER testTrigger
      |          |AFTER DELETE ON test
      |          |BEGIN
      |          |INSERT INTO test VALUES ("1", "2");
      |          |END
      |          ""${'"'}.trimMargin(), 0)
      |      return QueryResult.Unit
      |    }
      |
      |    private fun migrateInternal(
      |      driver: SqlDriver,
      |      oldVersion: Long,
      |      newVersion: Long,
      |    ): QueryResult.Value<Unit> {
      |      if (oldVersion <= 1 && newVersion > 1) {
      |        driver.execute(null, "ALTER TABLE test ADD COLUMN value2 TEXT", 0)
      |        driver.execute(null, "CREATE INDEX testIndex ON test(value)", 0)
      |        driver.execute(null, ""${'"'}
      |            |CREATE TRIGGER testTrigger
      |            |AFTER DELETE ON test
      |            |BEGIN
      |            |INSERT INTO test VALUES ("1", "2");
      |            |END
      |            ""${'"'}.trimMargin(), 0)
      |        driver.execute(null, ""${'"'}
      |            |CREATE VIEW testView AS
      |            |SELECT *
      |            |FROM test
      |            ""${'"'}.trimMargin(), 0)
      |      }
      |      return QueryResult.Unit
      |    }
      |
      |    override fun migrate(
      |      driver: SqlDriver,
      |      oldVersion: Long,
      |      newVersion: Long,
      |      vararg callbacks: AfterVersion,
      |    ): QueryResult.Value<Unit> {
      |      var lastVersion = oldVersion
      |
      |      callbacks.filter { it.afterVersion in oldVersion until newVersion }
      |      .sortedBy { it.afterVersion }
      |      .forEach { callback ->
      |        migrateInternal(driver, oldVersion = lastVersion, newVersion = callback.afterVersion + 1)
      |        callback.block(driver)
      |        lastVersion = callback.afterVersion + 1
      |      }
      |
      |      if (lastVersion < newVersion) {
      |        migrateInternal(driver, lastVersion, newVersion)
      |      }
      |      return QueryResult.Unit
      |    }
      |  }
      |}
      |
      """.trimMargin(),
    )
  }
}
