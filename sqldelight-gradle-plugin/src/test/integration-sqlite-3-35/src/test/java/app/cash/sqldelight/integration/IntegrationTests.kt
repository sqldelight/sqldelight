package app.cash.sqldelight.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class IntegrationTests {
  private lateinit var personQueries: PersonQueries
  private lateinit var keysetPagingQueries: KeysetPagingQueries
  private lateinit var windowsFunctionsQueries: WindowsFunctionsQueries

  @Before fun before() {
    val database = JdbcSqliteDriver(IN_MEMORY)
    QueryWrapper.Schema.create(database)

    val queryWrapper = QueryWrapper(database)
    personQueries = queryWrapper.personQueries
    keysetPagingQueries = queryWrapper.keysetPagingQueries
    windowsFunctionsQueries = queryWrapper.windowsFunctionsQueries
  }

  @Test fun insertReturning1() {
    assertThat(personQueries.insertAndReturn1(1, "Alec", "Strong").executeAsOne())
      .isEqualTo("Alec")
  }

  @Test fun insertReturningMany() {
    assertThat(personQueries.insertAndReturnMany(1, "Alec", "Strong").executeAsOne())
      .isEqualTo(InsertAndReturnMany(1, "Alec"))
  }

  @Test fun insertReturningAll() {
    assertThat(personQueries.insertAndReturnAll(1, "Alec", "Strong").executeAsOne())
      .isEqualTo(
        Person(1, "Alec", "Strong"),
      )
  }

  @Test fun updateReturning1() {
    personQueries.insertAndReturn1(1, "Alec", "Strong").executeAsOne()
    assertThat(personQueries.updateAndReturn1("Weak", "Strong").executeAsOne())
      .isEqualTo("Alec")
  }

  @Test fun updateReturningMany() {
    personQueries.insertAndReturn1(1, "Alec", "Strong").executeAsOne()
    assertThat(personQueries.updateAndReturnMany("Weak", "Strong").executeAsOne())
      .isEqualTo(UpdateAndReturnMany(1, "Alec"))
  }

  @Test fun updateReturningAll() {
    personQueries.insertAndReturn1(1, "Alec", "Strong").executeAsOne()
    assertThat(personQueries.updateAndReturnAll("Weak", "Strong").executeAsOne())
      .isEqualTo(Person(1, "Alec", "Weak"))
  }

  @Test fun deleteReturning1() {
    personQueries.insertAndReturn1(1, "Alec", "Strong").executeAsOne()
    assertThat(personQueries.deleteAndReturn1("Strong").executeAsOne())
      .isEqualTo("Alec")
  }

  @Test fun deleteReturningMany() {
    personQueries.insertAndReturn1(1, "Alec", "Strong").executeAsOne()
    assertThat(personQueries.deleteAndReturnMany("Strong").executeAsOne())
      .isEqualTo(DeleteAndReturnMany(1, "Alec"))
  }

  @Test fun deleteReturningAll() {
    personQueries.insertAndReturn1(1, "Alec", "Strong").executeAsOne()
    assertThat(personQueries.deleteAndReturnAll("Strong").executeAsOne())
      .isEqualTo(Person(1, "Alec", "Strong"))
  }

  @Test fun upsertWithMultipleConflict() {
    personQueries.performUpsert(1, "First", "Last")
    personQueries.performUpsert(1, "Alpha", "Omega")
    assertThat(personQueries.deleteAndReturnAll("Omega").executeAsOne())
      .isEqualTo(Person(1, "Alpha", "Omega"))
  }

  @Test fun keysetPagingWindowFunction() {
    keysetPagingQueries.insert()
    assertThat(keysetPagingQueries.page(limit = 5, anchor = 3).executeAsList().last()).isEqualTo(6)
  }

  @Test fun basicWindowFunctions() {
    windowsFunctionsQueries.insert()
    with(windowsFunctionsQueries.select().executeAsList()) {
      assertThat(last().a).isEqualTo("c")
      assertThat(last().row_number).isEqualTo(6)
      assertThat(last().rank).isEqualTo(5)
      assertThat(last().dense_rank).isEqualTo(3)
      assertThat(last().percent_rank).isEqualTo(0.8)
      assertThat(last().cume_dist).isEqualTo(1.0)
      assertThat(last().ntile).isEqualTo(2)
    }
  }

  @Test fun exprWindowFunctions() {
    windowsFunctionsQueries.insert()
    with(windowsFunctionsQueries.selectExpr().executeAsList()) {
      assertThat(last().b).isEqualTo("two")
      assertThat(last().lead).isEqualTo("n/a")
      assertThat(last().lag).isEqualTo("three")
      assertThat(last().first_value).isEqualTo("five")
      assertThat(last().last_value).isEqualTo("two")
      assertThat(last().nth_value).isEqualTo("one")
    }
  }

  @Test fun groupWindowFunctions() {
    windowsFunctionsQueries.insertGroup()
    with(windowsFunctionsQueries.selectGroup().executeAsList()) {
      assertThat(last().a).isEqualTo(7)
      assertThat(last().b).isEqualTo("G")
      assertThat(last().group_concat).isEqualTo("F.G")
    }
  }
}
