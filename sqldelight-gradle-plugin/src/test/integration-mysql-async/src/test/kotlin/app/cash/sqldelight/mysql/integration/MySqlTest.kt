package app.cash.sqldelight.mysql.integration

import app.cash.sqldelight.coroutines.await
import app.cash.sqldelight.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.awaitAsOne
import app.cash.sqldelight.driver.r2dbc.R2dbcDriver
import com.google.common.truth.Truth.assertThat
import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.reactive.awaitSingle
import org.junit.Test

class MySqlTest {
  private val factory = ConnectionFactories.get("r2dbc:tc:mysql:///myDb?TC_IMAGE_TAG=8.0")

  private fun runTest(block: suspend (MyDatabase) -> Unit) = kotlinx.coroutines.test.runTest {
    val db = before()
    block(db)
  }

  suspend fun before(): MyDatabase {
    val connection = factory.create().awaitSingle()
    val driver = R2dbcDriver(connection)

    return MyDatabase(driver).also { MyDatabase.Schema.create(driver).await() }
  }

  fun after() {
  }

  @Test fun simpleSelect() = runTest { database ->
    database.dogQueries.insertDog("Tilda", "Pomeranian", true).await()
    assertThat(database.dogQueries.selectDogs().awaitAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = true
        )
      )
  }

  @Test
  fun simpleSelectWithIn() = runTest { database ->
    with(database) {
      dogQueries.insertDog("Tilda", "Pomeranian", true).await()
      dogQueries.insertDog("Tucker", "Portuguese Water Dog", true).await()
      dogQueries.insertDog("Cujo", "Pomeranian", false).await()
      dogQueries.insertDog("Buddy", "Pomeranian", true).await()
      assertThat(
        dogQueries.selectDogsByBreedAndNames(
          breed = "Pomeranian",
          name = listOf("Tilda", "Buddy")
        ).awaitAsList()
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
  }

  /*@Test
  fun testDates() = runTest { database ->
    database.datesQueries.insertDate(
            date = LocalDate.of(2020, 1, 1),
            time = LocalTime.of(21, 30, 59),
            datetime = LocalDateTime.of(2020, 1, 1, 21, 30, 59),
            timestamp = OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
            year = "2022"
    ).await()
    assertThat(
      database.datesQueries.selectDate().awaitAsOne()
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
  }*/
}
