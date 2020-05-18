package com.squareup.sqldelight.postgresql.integration

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.sqlite.driver.JdbcDriver
import java.sql.DriverManager
import org.junit.After
import org.junit.Before
import org.junit.Test

class PostgreSqlTest {
  val conn = DriverManager.getConnection("jdbc:tc:postgresql:9.6.8:///my_db")
  val driver = object : JdbcDriver() {
    override fun getConnection() = conn
  }
  val database = MyDatabase(driver)

  @Before fun before() {
    MyDatabase.Schema.create(driver)
  }

  @After fun after() {
    conn.close()
  }

  @Test fun simpleSelect() {
    database.dogQueries.insertDog("Tilda", "Pomeranian", true)
    assertThat(database.dogQueries.selectDogs().executeAsOne())
      .isEqualTo(Dog.Impl(
        name = "Tilda",
        breed = "Pomeranian",
        is_good = true
      ))
  }
}
