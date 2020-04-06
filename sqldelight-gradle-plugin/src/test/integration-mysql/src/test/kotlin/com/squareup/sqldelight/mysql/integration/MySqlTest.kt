package com.squareup.sqldelight.mysql.integration

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.sqlite.driver.JdbcDriver
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

class MySqlTest {
    val conn = DriverManager.getConnection("jdbc:mysql://localhost:3306?serverTimezone=UTC&user=root&password=root")
    val driver = object : JdbcDriver() {
        override fun getConnection() = conn
    }
    val database = MyDatabase(driver)

    @Before fun before() {
        conn.prepareStatement("CREATE DATABASE myDb;").execute()
        conn.prepareStatement("USE myDb;").execute()
        MyDatabase.Schema.create(driver)
    }

    @After fun after() {
        conn.prepareStatement("DROP DATABASE IF EXISTS myDb;").execute()
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
