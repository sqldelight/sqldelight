package app.cash.sqldelight.mysql.integration

import app.cash.sqldelight.driver.r2dbc.R2dbcDriver
import com.google.common.truth.Truth.assertThat
import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.reactive.awaitSingle
import org.junit.Test

class MySqlTest {
  private val factory = ConnectionFactories.get("r2dbc:tc:mysql:///myDb?TC_IMAGE_TAG=8.0")

  private fun runTest(block: suspend (MyDatabase) -> Unit) = kotlinx.coroutines.test.runTest {
    val connection = factory.create().awaitSingle()
    val driver = R2dbcDriver(connection)

    val db = MyDatabase(driver).also { MyDatabase.Schema.create(driver) }
    block(db)
  }

  @Test fun simpleSelect() = runTest { database ->
    database.dogQueries.insertDog("Tilda", "Pomeranian", true)
    assertThat(database.dogQueries.selectDogs().executeAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = true,
        ),
      )
  }

  @Test
  fun simpleSelectWithIn() = runTest { database ->
    with(database) {
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
  }
}
