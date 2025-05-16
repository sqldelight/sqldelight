package app.cash.sqldelight.mysql.integration

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import com.google.common.truth.Truth.assertThat
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MySqlTest {
  lateinit var connection: Connection
  lateinit var dogQueries: DogQueries
  lateinit var datesQueries: DatesQueries
  lateinit var driver: JdbcDriver

  @Before
  fun before() {
    connection = DriverManager.getConnection("jdbc:tc:mysql:8.0:///myDb")
    driver = object : JdbcDriver() {
      override fun getConnection() = connection
      override fun closeConnection(connection: Connection) = Unit
      override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit
      override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit
      override fun notifyListeners(vararg queryKeys: String) = Unit
    }
    val database = MyDatabase(driver)

    MyDatabase.Schema.create(driver)
    dogQueries = database.dogQueries
    datesQueries = database.datesQueries
  }

  @After
  fun after() {
    connection.close()
  }

  @Test fun simpleSelect() {
    dogQueries.insertDog("Tilda", "Pomeranian", true)
    assertThat(dogQueries.selectDogs().executeAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = true,
        ),
      )
  }

  @Test
  fun simpleSelectWithIn() {
    dogQueries.insertDog("Tilda", "Pomeranian", true)
    dogQueries.insertDog("Tucker", "Portuguese Water Dog", true)
    dogQueries.insertDog("Cujo", "Pomeranian", false)
    dogQueries.insertDog("Buddy", "Pomeranian", true)
    assertThat(
      dogQueries.selectDogsByBreedAndNames(
        breed = "Pomeranian",
        name = listOf("Tilda", "Buddy"),
      ).executeAsList(),
    )
      .containsExactly(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = true,
        ),
        Dog(
          name = "Buddy",
          breed = "Pomeranian",
          is_good = true,
        ),
      )
  }

  @Test
  fun testDates() {
    with(
      datesQueries.insertDate(
        date = LocalDate.of(2020, 1, 1),
        time = LocalTime.of(21, 30, 59),
        datetime = LocalDateTime.of(2020, 1, 1, 21, 30, 59),
        timestamp = OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
        year = "2022",
      ).executeAsOne(),
    ) {
      assertThat(date).isEqualTo(LocalDate.of(2020, 1, 1))
      assertThat(time).isEqualTo(LocalTime.of(21, 30, 59))
      assertThat(datetime).isEqualTo(LocalDateTime.of(2020, 1, 1, 21, 30, 59))

      assertThat(timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE))
        .isEqualTo(OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)).format(DateTimeFormatter.ISO_LOCAL_DATE))
      assertThat(year).isEqualTo("2022-01-01")
    }
  }

  @Test
  fun transactionCrashRollsBack() {
    val transacter = SqlDriverTransacter(driver)

    try {
      transacter.transaction {
        driver.execute(null, "CREATE TABLE throw_test(some Text)", 0, null)
        afterRollback { driver.execute(null, "DROP TABLE throw_test", 0, null) }
        throw ExpectedException()
      }
      Assert.fail()
    } catch (_: ExpectedException) {
      transacter.transaction {
        driver.execute(null, "CREATE TABLE throw_test(some Text)", 0, null)
      }
    }
  }

  private class ExpectedException : Exception()
  private class SqlDriverTransacter(driver: SqlDriver) : TransacterImpl(driver)
}
