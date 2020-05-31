package com.squareup.sqldelight.mysql.integration

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.sqlite.driver.JdbcDriver
import java.sql.Connection
import java.sql.DriverManager
import org.junit.After
import org.junit.Before
import org.junit.Test

class MySqlTest {
    lateinit var connection: Connection
    lateinit var dogQueries: DogQueries

    @Before
    fun before() {
        connection = DriverManager.getConnection("jdbc:tc:mysql:///myDb")
        val driver = object : JdbcDriver() {
            override fun getConnection() = connection
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
            .isEqualTo(Dog(
                name = "Tilda",
                breed = "Pomeranian",
                is_good = true
            ))
    }

    @Test
    fun simpleSelectWithIn() {
        dogQueries.insertDog("Tilda", "Pomeranian", true)
        dogQueries.insertDog("Tucker", "Portuguese Water Dog", true)
        dogQueries.insertDog("Cujo", "Pomeranian", false)
        dogQueries.insertDog("Buddy", "Pomeranian", true)
        assertThat(dogQueries.selectDogsByBreedAndNames(
            breed = "Pomeranian",
            name = listOf("Tilda", "Buddy")
        ).executeAsList())
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
