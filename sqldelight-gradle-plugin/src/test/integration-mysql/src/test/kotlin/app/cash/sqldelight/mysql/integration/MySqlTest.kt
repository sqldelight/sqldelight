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
  lateinit var charactersQueries: CharactersQueries
  lateinit var numbersQueries: NumbersQueries
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
    charactersQueries = database.charactersQueries
    numbersQueries = database.numbersQueries
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
    val result: String = datesQueries.getNow().executeAsOne()
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
  fun testDatesMinMax() {
    datesQueries.insertDate(
      date = LocalDate.of(2020, 1, 1),
      time = LocalTime.of(21, 30, 59),
      datetime = LocalDateTime.of(2020, 1, 1, 21, 30, 59),
      timestamp = OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
      year = "2022",
    ).executeAsOne()

    with(
      datesQueries.minDates().executeAsOne(),
    ) {
      assertThat(minDate).isEqualTo(LocalDate.of(2020, 1, 1))
      assertThat(minTime).isEqualTo(LocalTime.of(21, 30, 59))
      assertThat(minDatetime).isEqualTo(LocalDateTime.of(2020, 1, 1, 21, 30, 59))

      assertThat(minTimestamp?.format(DateTimeFormatter.ISO_LOCAL_DATE))
        .isEqualTo(OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)).format(DateTimeFormatter.ISO_LOCAL_DATE))
      assertThat(minYear).isEqualTo("2022-01-01")
    }

    with(
      datesQueries.maxDates().executeAsOne(),
    ) {
      assertThat(maxDate).isEqualTo(LocalDate.of(2020, 1, 1))
      assertThat(maxTime).isEqualTo(LocalTime.of(21, 30, 59))
      assertThat(maxDatetime).isEqualTo(LocalDateTime.of(2020, 1, 1, 21, 30, 59))

      assertThat(maxTimestamp?.format(DateTimeFormatter.ISO_LOCAL_DATE))
        .isEqualTo(OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)).format(DateTimeFormatter.ISO_LOCAL_DATE))
      assertThat(maxYear).isEqualTo("2022-01-01")
    }
  }

  @Test
  fun testIntsMinMaxSum() {
    with(
      numbersQueries.sumInts().executeAsOne(),
    ) {
      assertThat(sumTiny).isNull()
      assertThat(sumSmall).isNull()
      assertThat(sumInt).isNull()
      assertThat(sumBig).isNull()
    }

    with(
      numbersQueries.minInts().executeAsOne(),
    ) {
      assertThat(minTiny).isNull()
      assertThat(minSmall).isNull()
      assertThat(minInt).isNull()
      assertThat(minBig).isNull()
    }

    with(
      numbersQueries.maxInts().executeAsOne(),
    ) {
      assertThat(maxTiny).isNull()
      assertThat(maxSmall).isNull()
      assertThat(maxInt).isNull()
      assertThat(maxBig).isNull()
    }

    numbersQueries.insertInts(
      tinyint = 1,
      smallint = 1,
      integer = 1,
      bigint = 1,
    )
    numbersQueries.insertInts(
      tinyint = 2,
      smallint = 2,
      integer = 2,
      bigint = 2,
    )

    with(
      numbersQueries.sumInts().executeAsOne(),
    ) {
      assertThat(sumTiny).isInstanceOf(Long::class.javaObjectType)
      assertThat(sumSmall).isInstanceOf(Long::class.javaObjectType)
      assertThat(sumInt).isInstanceOf(Long::class.javaObjectType)
      assertThat(sumBig).isInstanceOf(Long::class.javaObjectType)
      assertThat(sumTiny).isEqualTo(3)
      assertThat(sumSmall).isEqualTo(3)
      assertThat(sumInt).isEqualTo(3)
      assertThat(sumBig).isEqualTo(3)
    }

    with(
      numbersQueries.minInts().executeAsOne(),
    ) {
      assertThat(minTiny).isEqualTo(1)
      assertThat(minSmall).isEqualTo(1)
      assertThat(minInt).isEqualTo(1)
      assertThat(minBig).isEqualTo(1)
    }

    with(
      numbersQueries.maxInts().executeAsOne(),
    ) {
      assertThat(maxTiny).isEqualTo(2)
      assertThat(maxSmall).isEqualTo(2)
      assertThat(maxInt).isEqualTo(2)
      assertThat(maxBig).isEqualTo(2)
    }
  }

  @Test
  fun testMultiplySmallerIntsBecomeLongs() {
    numbersQueries.insertInts(
      tinyint = 1,
      smallint = 1,
      integer = 1,
      bigint = Long.MAX_VALUE,
    )

    with(
      numbersQueries.multiplyWithBigInts().executeAsOne(),
    ) {
      assertThat(tiny).isEqualTo(Long.MAX_VALUE)
      assertThat(small).isEqualTo(Long.MAX_VALUE)
      assertThat(integer).isEqualTo(Long.MAX_VALUE)
    }
  }

  @Test
  fun testFloat() {
    with(
      numbersQueries.sumMinMaxFloat().executeAsOne(),
    ) {
      assertThat(sumFloat).isNull()
      assertThat(minFloat).isNull()
      assertThat(maxFloat).isNull()
    }

    numbersQueries.insertFloats(
      float = 1.5,
    )

    with(
      numbersQueries.sumMinMaxFloat().executeAsOne(),
    ) {
      assertThat(sumFloat).isEqualTo(1.5)
      assertThat(minFloat).isEqualTo(1.5)
      assertThat(maxFloat).isEqualTo(1.5)
    }
  }

  @Test
  fun testMultiplyFloatInt() {
    numbersQueries.insertInts(
      tinyint = 3,
      smallint = 3,
      integer = 3,
      bigint = 3,
    )

    numbersQueries.insertFloats(
      float = 1.5,
    )

    with(
      numbersQueries.multiplyFloatInt().executeAsOne(),
    ) {
      assertThat(mul).isEqualTo(4.5)
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

  @Test fun lengthFunctionReturnsByteCount() {
    charactersQueries.insertCharacter("海豚", null)
    val length = charactersQueries.selectNameLength().executeAsOne()
    assertThat(length).isEqualTo(6)
    val nullLength = charactersQueries.selectDescriptionLength().executeAsOne()
    assertThat(nullLength.length).isNull()
  }

  @Test fun charLengthFunctionReturnsCharacterCount() {
    charactersQueries.insertCharacter("海豚", null)
    val length = charactersQueries.selectNameCharLength().executeAsOne()
    assertThat(length).isEqualTo(2)
    val nullLength = charactersQueries.selectDescriptionCharLength().executeAsOne()
    assertThat(nullLength.char_length).isNull()
  }

  private class ExpectedException : Exception()
  private class SqlDriverTransacter(driver: SqlDriver) : TransacterImpl(driver)
}
