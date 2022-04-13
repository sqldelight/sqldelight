package app.cash.sqldelight.mysql.integration

import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class MySqlTest {
  lateinit var connection: Connection
  lateinit var dogQueries: DogQueries
  lateinit var datesQueries: DatesQueries

  @Before
  fun before() {
    connection = DriverManager.getConnection("jdbc:tc:mysql:///myDb")
    val driver = object : JdbcDriver() {
      override fun getConnection() = connection
      override fun closeConnection(connection: Connection) = Unit
      override fun addListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
      override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
      override fun notifyListeners(queryKeys: Array<String>) = Unit
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
          is_good = true
        )
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
        name = listOf("Tilda", "Buddy")
      ).executeAsList()
    )
      .containsExactly(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = true
        ),
        Dog(
          name = "Buddy",
          breed = "Pomeranian",
          is_good = true
        )
      )
  }

  @Test
  fun testDates() {
    assertThat(
      datesQueries.insertDate(
        date = LocalDate.of(2020, 1, 1),
        time = LocalTime.of(21, 30, 59),
        datetime = LocalDateTime.of(2020, 1, 1, 21, 30, 59),
        timestamp = OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
        year = "2022"
      ).executeAsOne()
    )
      .isEqualTo(
        Dates(
          date = LocalDate.of(2020, 1, 1),
          time = LocalTime.of(21, 30, 59),
          datetime = LocalDateTime.of(2020, 1, 1, 21, 30, 59),
          timestamp = OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
          year = "2022-01-01"
        )
      )
  }
}
