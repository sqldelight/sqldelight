package com.squareup.sqldelight.integration

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.db.AfterVersion
import com.squareup.sqldelight.db.AfterVersionWithParameter
import com.squareup.sqldelight.db.migrateWithCallbacks
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver.Companion.IN_MEMORY
import org.junit.Test

class IntegrationTests {
  @Test fun migrationCallbacks() {
    val database = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    QueryWrapper.Schema.migrateWithCallbacks(
      driver = database,
      oldVersion = 0,
      newVersion = QueryWrapper.Schema.version,
      AfterVersion(1) { database.execute(null, "INSERT INTO test (value) VALUES('hello')", 0) },
      AfterVersion(2) { database.execute(null, "INSERT INTO test2 (value, value2) VALUES('hello2', 'sup2')", 0) },
      AfterVersion(3) { database.execute(null, "INSERT INTO test2 (new_value, new_value2) VALUES('hello3', 'sup3')", 0) },
    )

    val queryWrapper = QueryWrapper(database)
    val values = queryWrapper.testQueries.selectAll().executeAsList()
    assertThat(values).containsExactly(
      Test2(new_value = "hello", new_value2 = "sup"),
      Test2(new_value = "hello2", new_value2 = "sup2"),
      Test2(new_value = "hello3", new_value2 = "sup3"),
    )
  }

  @Test fun migrationCallbacksWithParameter() {
    val database = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    QueryWrapper.Schema.migrateWithCallbacks(
      driver = database,
      oldVersion = 0,
      newVersion = QueryWrapper.Schema.version,
      AfterVersionWithParameter(1) { it.execute(null, "INSERT INTO test (value) VALUES('hello')", 0) },
      AfterVersionWithParameter(2) { it.execute(null, "INSERT INTO test2 (value, value2) VALUES('hello2', 'sup2')", 0) },
      AfterVersionWithParameter(3) { it.execute(null, "INSERT INTO test2 (new_value, new_value2) VALUES('hello3', 'sup3')", 0) },
    )

    val queryWrapper = QueryWrapper(database)
    val values = queryWrapper.testQueries.selectAll().executeAsList()
    assertThat(values).containsExactly(
      Test2(new_value = "hello", new_value2 = "sup"),
      Test2(new_value = "hello2", new_value2 = "sup2"),
      Test2(new_value = "hello3", new_value2 = "sup3"),
    )
  }

  @Test fun migrationCallbackLessThanOldVersionNotCalled() {
    val database = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    QueryWrapper.Schema.migrate(
      driver = database,
      oldVersion = 0,
      newVersion = 2
    )
    QueryWrapper.Schema.migrateWithCallbacks(
      driver = database,
      oldVersion = 2,
      newVersion = QueryWrapper.Schema.version,
      AfterVersion(1) { database.execute(null, "INSERT INTO test (value) VALUES('hello')", 0) },
      AfterVersion(2) { database.execute(null, "INSERT INTO test2 (value, value2) VALUES('hello2', 'sup2')", 0) },
      AfterVersion(3) { database.execute(null, "INSERT INTO test2 (new_value, new_value2) VALUES('hello3', 'sup3')", 0) },
    )

    val queryWrapper = QueryWrapper(database)
    val values = queryWrapper.testQueries.selectAll().executeAsList()
    assertThat(values).containsExactly(
      Test2(new_value = "hello2", new_value2 = "sup2"),
      Test2(new_value = "hello3", new_value2 = "sup3"),
    )
  }

  @Test fun migrationCallbackLessThanOldVersionNotCalledWithParameter() {
    val database = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    QueryWrapper.Schema.migrate(
      driver = database,
      oldVersion = 0,
      newVersion = 2
    )
    QueryWrapper.Schema.migrateWithCallbacks(
      driver = database,
      oldVersion = 2,
      newVersion = QueryWrapper.Schema.version,
      AfterVersionWithParameter(1) { it.execute(null, "INSERT INTO test (value) VALUES('hello')", 0) },
      AfterVersionWithParameter(2) { it.execute(null, "INSERT INTO test2 (value, value2) VALUES('hello2', 'sup2')", 0) },
      AfterVersionWithParameter(3) { it.execute(null, "INSERT INTO test2 (new_value, new_value2) VALUES('hello3', 'sup3')", 0) },
    )

    val queryWrapper = QueryWrapper(database)
    val values = queryWrapper.testQueries.selectAll().executeAsList()
    assertThat(values).containsExactly(
      Test2(new_value = "hello2", new_value2 = "sup2"),
      Test2(new_value = "hello3", new_value2 = "sup3"),
    )
  }

  @Test fun migrationCallbackGreaterThanNewVersionNotCalled() {
    val database = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    QueryWrapper.Schema.migrateWithCallbacks(
      driver = database,
      oldVersion = 0,
      newVersion = QueryWrapper.Schema.version,
      AfterVersion(1) { database.execute(null, "INSERT INTO test (value) VALUES('hello')", 0) },
      AfterVersion(2) { database.execute(null, "INSERT INTO test2 (value, value2) VALUES('hello2', 'sup2')", 0) },
      AfterVersion(3) { database.execute(null, "INSERT INTO test2 (new_value, new_value2) VALUES('hello3', 'sup3')", 0) },
      AfterVersion(4) { database.execute(null, "INSERT INTO test2 (new_value, new_value2) VALUES('hello4', 'sup4')", 0) },
    )

    val queryWrapper = QueryWrapper(database)
    val values = queryWrapper.testQueries.selectAll().executeAsList()
    assertThat(values).containsExactly(
      Test2(new_value = "hello", new_value2 = "sup"),
      Test2(new_value = "hello2", new_value2 = "sup2"),
      Test2(new_value = "hello3", new_value2 = "sup3"),
    )
  }

  @Test fun migrationCallbackGreaterThanNewVersionNotCalledWithParameter() {
    val database = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    QueryWrapper.Schema.migrateWithCallbacks(
      driver = database,
      oldVersion = 0,
      newVersion = QueryWrapper.Schema.version,
      AfterVersionWithParameter(1) { it.execute(null, "INSERT INTO test (value) VALUES('hello')", 0) },
      AfterVersionWithParameter(2) { it.execute(null, "INSERT INTO test2 (value, value2) VALUES('hello2', 'sup2')", 0) },
      AfterVersionWithParameter(3) { it.execute(null, "INSERT INTO test2 (new_value, new_value2) VALUES('hello3', 'sup3')", 0) },
      AfterVersionWithParameter(4) { it.execute(null, "INSERT INTO test2 (new_value, new_value2) VALUES('hello4', 'sup4')", 0) },
    )

    val queryWrapper = QueryWrapper(database)
    val values = queryWrapper.testQueries.selectAll().executeAsList()
    assertThat(values).containsExactly(
      Test2(new_value = "hello", new_value2 = "sup"),
      Test2(new_value = "hello2", new_value2 = "sup2"),
      Test2(new_value = "hello3", new_value2 = "sup3"),
    )
  }

}
