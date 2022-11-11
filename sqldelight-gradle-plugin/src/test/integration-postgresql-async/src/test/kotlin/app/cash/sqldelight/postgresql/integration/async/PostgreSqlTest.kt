package app.cash.sqldelight.postgresql.integration.async

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.OptimisticLockException
import app.cash.sqldelight.driver.r2dbc.R2dbcDriver
import com.google.common.truth.Truth.assertThat
import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.reactive.awaitSingle
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class PostgreSqlTest {
  private val factory = ConnectionFactories.get("r2dbc:tc:postgresql:///myDb?TC_IMAGE_TAG=9.6.8")

  private fun runTest(block: suspend (MyDatabase) -> Unit) = kotlinx.coroutines.test.runTest {
    val connection = factory.create().awaitSingle()
    val driver = R2dbcDriver(connection, replaceParameter = true)
    val db = MyDatabase(driver)
    MyDatabase.Schema.awaitCreate(driver)
    block(db)
  }

  @Test fun simpleSelect() = runTest { database ->
    Assert.assertEquals(0, database.dogQueries.selectDogs().awaitAsList().size)
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    assertThat(database.dogQueries.selectDogs().awaitAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = 1,
        ),
      )
  }

  @Test fun booleanSelect() = runTest { database ->
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    assertThat(database.dogQueries.selectGoodDogs(true).awaitAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = 1,
        ),
      )
  }

  @Test fun returningInsert() = runTest { database ->
    assertThat(database.dogQueries.insertAndReturn("Tilda", "Pomeranian").awaitAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = 1,
        ),
      )
  }

  @Test fun testDates() = runTest { database ->
    assertThat(
      database.datesQueries.insertDate(
        date = LocalDate.of(2020, 1, 1),
        time = LocalTime.of(21, 30, 59, 10000),
        timestamp = LocalDateTime.of(2020, 1, 1, 21, 30, 59, 10000),
        timestamp_with_timezone = OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
      ).awaitAsOne(),
    )
      .isEqualTo(
        Dates(
          date = LocalDate.of(2020, 1, 1),
          time = LocalTime.of(21, 30, 59, 10000),
          timestamp = LocalDateTime.of(2020, 1, 1, 21, 30, 59, 10000),
          timestamp_with_timezone = OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
        ),
      )
  }

  @Test fun testDateTrunc() = runTest { database ->
    database.datesQueries.insertDate(
      date = LocalDate.of(2020, 1, 1),
      time = LocalTime.of(21, 30, 59, 10000),
      timestamp = LocalDateTime.of(2020, 1, 1, 21, 30, 59, 10000),
      timestamp_with_timezone = OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
    ).awaitAsOne()

    assertThat(
      database.datesQueries.selectDateTrunc().awaitAsOne(),
    )
      .isEqualTo(
        SelectDateTrunc(
          date_trunc = LocalDateTime.of(2020, 1, 1, 21, 0, 0, 0),
          date_trunc_ = OffsetDateTime.of(1980, 4, 9, 20, 0, 0, 0, ZoneOffset.ofHours(0)),
        ),
      )
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
