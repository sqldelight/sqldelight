package app.cash.sqldelight.mysql.integration

import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

class MySqlTest {
  lateinit var connection: Connection
  lateinit var dogQueries: DogQueries

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
}
