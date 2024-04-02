package app.cash.sqldelight.postgresql.integration.async

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.OptimisticLockException
import app.cash.sqldelight.driver.r2dbc.R2dbcDriver
import com.google.common.truth.Truth.assertThat
import io.r2dbc.spi.ConnectionFactories
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.test.TestResult
import org.junit.Assert
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.PostgreSQLR2DBCDatabaseContainer

class PostgreSqlTest {

  private fun runTest(block: suspend (MyDatabase) -> Unit): TestResult {
    val container = PostgreSQLContainer("postgres:9.6.8").apply {
      withDatabaseName("myDb")
    }
    container.start()
    return container.use {
      val connectionFactory = ConnectionFactories.get(PostgreSQLR2DBCDatabaseContainer.getOptions(it))
      kotlinx.coroutines.test.runTest {
        val connection = connectionFactory.create().awaitSingle()
        val awaitClose = CompletableDeferred<Unit>()
        val driver = R2dbcDriver(connection) {
          if (it == null) {
            awaitClose.complete(Unit)
          } else {
            awaitClose.completeExceptionally(it)
          }
        }
        val db = MyDatabase(driver)
        MyDatabase.Schema.awaitCreate(driver)
        block(db)
        driver.close()
        awaitClose.join()
      }
    }
  }

  @Test fun simpleSelectWithNullPrimitive() = runTest { database ->
    Assert.assertEquals(0, database.dogQueries.selectDogs().awaitAsList().size)
    database.dogQueries.insertDog("Tilda", "Pomeranian", null)
    assertThat(database.dogQueries.selectDogs().awaitAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = 1,
          age = null,
        ),
      )
  }

  @Test fun getConnectionUsingCoroutineOverload(): TestResult {
    val container = PostgreSQLContainer("postgres:9.6.8").apply {
      withDatabaseName("myDb")
    }
    container.start()
    return container.use {
      val connectionFactory = ConnectionFactories.get(PostgreSQLR2DBCDatabaseContainer.getOptions(it))
      kotlinx.coroutines.test.runTest {
        val connection = connectionFactory.create().awaitSingle()
        R2dbcDriver(connection).use { driver ->
          assertThat(driver.connection).isEqualTo(connection)
        }
      }
    }
  }

  @Test fun booleanSelect() = runTest { database ->
    database.dogQueries.insertDog("Tilda", "Pomeranian", null)
    assertThat(database.dogQueries.selectGoodDogs(true).awaitAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = 1,
          age = null,
        ),
      )
  }

  @Test fun returningInsert() = runTest { database ->
    assertThat(database.dogQueries.insertAndReturn("Tilda", "Pomeranian", null).awaitAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = 1,
          age = null,
        ),
      )
  }

  @Test fun testDates() = runTest { database ->
    with(
      database.datesQueries.insertDate(
        date = LocalDate.of(2020, 1, 1),
        time = LocalTime.of(21, 30, 59, 10000),
        timestamp = LocalDateTime.of(2020, 1, 1, 21, 30, 59, 10000),
        timestamp_with_timezone = OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
      ).awaitAsOne(),
    ) {
      assertThat(date).isEqualTo(LocalDate.of(2020, 1, 1))
      assertThat(time).isEqualTo(LocalTime.of(21, 30, 59, 10000))
      assertThat(timestamp).isEqualTo(LocalDateTime.of(2020, 1, 1, 21, 30, 59, 10000))
      assertThat(timestamp_with_timezone.format(DateTimeFormatter.ISO_LOCAL_DATE))
        .isEqualTo(OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)).format(DateTimeFormatter.ISO_LOCAL_DATE))
    }
  }

  @Test fun testDateTrunc() = runTest { database ->
    database.datesQueries.insertDate(
      date = LocalDate.of(2020, 1, 1),
      time = LocalTime.of(21, 30, 59, 10000),
      timestamp = LocalDateTime.of(2020, 1, 1, 21, 30, 59, 10000),
      timestamp_with_timezone = OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
    ).awaitAsOne()

    with(
      database.datesQueries.selectDateTrunc().awaitAsOne(),
    ) {
      assertThat(date_trunc).isEqualTo(LocalDateTime.of(2020, 1, 1, 21, 0, 0, 0))
      assertThat(date_trunc_.format(DateTimeFormatter.ISO_LOCAL_DATE))
        .isEqualTo(OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)).format(DateTimeFormatter.ISO_LOCAL_DATE))
    }
  }

  @Test fun testSerial() = runTest { database ->
    database.run {
      oneEntityQueries.transaction {
        oneEntityQueries.insert("name1")
        oneEntityQueries.insert("name2")
        oneEntityQueries.insert("name3")
      }
      assertThat(oneEntityQueries.selectAll().awaitAsList().map { it.id }).containsExactly(1, 2, 3)
    }
  }

  @Test fun testArrays() = runTest { database ->
    with(database.arraysQueries.insertAndReturn(arrayOf(1, 2), arrayOf("one", "two")).awaitAsOne()) {
      assertThat(intArray!!.asList()).containsExactly(1, 2).inOrder()
      assertThat(textArray!!.asList()).containsExactly("one", "two").inOrder()
    }
  }

  @Test fun now() = runTest { database ->
    val now = database.datesQueries.selectNow().awaitAsOne()
    assertThat(now).isNotNull()
    assertThat(now).isGreaterThan(OffsetDateTime.MIN)
  }

  @Test fun successfulOptimisticLock() = runTest { database ->
    with(database.withLockQueries) {
      val row = insertText("sup").awaitAsOne()

      updateText(
        id = row.id,
        version = row.version,
        text = "sup2",
      )

      assertThat(selectForId(row.id).awaitAsOne().text).isEqualTo("sup2")
    }
  }

  @Test fun unsuccessfulOptimisticLock() = runTest { database ->
    with(database.withLockQueries) {
      val row = insertText("sup").awaitAsOne()

      updateText(
        id = row.id,
        version = row.version,
        text = "sup2",
      )

      try {
        updateText(
          id = row.id,
          version = row.version,
          text = "sup3",
        )
        Assert.fail()
      } catch (e: OptimisticLockException) { }
    }
  }
}
