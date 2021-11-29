package com.squareup.sqldelight.mysql.integration

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.sqlite.driver.JdbcDriver
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

class MySqlTest {
  lateinit var connection: Connection
  lateinit var queries: DogsQueries

  @Before
  fun before() {
    connection = DriverManager.getConnection("jdbc:tc:mysql:///myDb")
    val driver = object : JdbcDriver() {
      override fun getConnection() = connection
      override fun closeConnection(connection: Connection) = Unit
      override fun addListener(listener: Query.Listener, vararg queryKeys: String) = Unit
      override fun removeListener(listener: Query.Listener, vararg queryKeys: String) = Unit
      override fun notifyListeners(vararg queryKeys: String) = Unit
    }
    val database = MyDatabase(driver)

    MyDatabase.Schema.create(driver)
    queries = database.dogsQueries
  }

  @After
  fun after() {
    connection.close()
  }

  @Test
  fun `select dogs for owner`() {
    queries.insertPerson(1, "Jeff")
    queries.insertPerson(2, "Jake")
    queries.insertDog("Iris", "Cat", 1, true)
    queries.insertDog("Hazel", "French Bulldog", 2, true)
    queries.insertDog("Olive", "French Bulldog", 2, true)

    assertThat(queries.selectDogsForOwnerName("Jake").executeAsList())
      .containsExactly(
        Dog(
          name = "Hazel",
          breed = "French Bulldog",
          owner = 2,
          is_good = true
        ),
        Dog(
          name = "Olive",
          breed = "French Bulldog",
          owner = 2,
          is_good = true
        )
      )
  }

  @Test
  fun `select bad name dogs`() {
    queries.insertPerson(1, "Jeff")
    queries.insertPerson(2, "Jake")
    queries.insertDog("Cat", "Cat", 1, true)
    queries.insertDog("Dog", "Dog", 2, true)
    queries.insertDog("Olive", "French Bulldog", 2, true)

    assertThat(queries.selectBadNameDogs().executeAsList())
      .containsExactly(
        Bad_name_dogs(
          name = "Cat",
          breed = "Cat"
        ),
        Bad_name_dogs(
          name = "Dog",
          breed = "Dog"
        )
      )
  }
}
