package com.squareup.sqldelight.postgresql.integration

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.sqlite.driver.JdbcDriver
import java.sql.Connection
import java.sql.DriverManager
import org.junit.After
import org.junit.Before
import org.junit.Test

class PostgreSqlTest {

  val connPostgres = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres")
  lateinit var connMyDb: Connection
  lateinit var driver: JdbcDriver
  lateinit var database: MyDatabase

  @Before
  fun before() {
    connPostgres.prepareStatement("CREATE DATABASE my_db;").execute()

    connMyDb = DriverManager.getConnection("jdbc:postgresql://localhost:5432/my_db")
    driver = object : JdbcDriver() {
      override fun getConnection() = connMyDb
    }
    MyDatabase.Schema.create(driver)
    database = MyDatabase(driver)
  }

  @After
  fun after() {
    connMyDb.close()

    connPostgres.prepareStatement("DROP DATABASE IF EXISTS my_db;").execute()
    connPostgres.close()
  }

  @Test
  fun simpleSelect() {
    database.dogQueries.insertDog("Tilda", "Pomeranian", true)
    assertThat(database.dogQueries.selectDogs().executeAsOne())
      .isEqualTo(Dog.Impl(
        name = "Tilda",
        breed = "Pomeranian",
        is_good = true
      ))
  }
}
