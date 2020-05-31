package com.squareup.sqldelight.mysql.integration

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.sqlite.driver.JdbcDriver
import java.sql.DriverManager
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class MySqlTest {
    @Before fun before() {
        dogQueries.deleteDogs()
    }

    @Test fun simpleSelect() {
        database.dogQueries.insertDog("Tilda", "Pomeranian", true)
        assertThat(database.dogQueries.selectDogs().executeAsOne())
            .isEqualTo(Dog(
                name = "Tilda",
                breed = "Pomeranian",
                is_good = true
            ))
    }

    @Test fun simpleSelectWithIn() {
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

    companion object {
        lateinit var dogQueries: DogQueries
        val conn = DriverManager.getConnection("jdbc:tc:mysql:///myDb")
        val driver = object : JdbcDriver() {
            override fun getConnection() = conn
        }
        val database = MyDatabase(driver)

        @BeforeClass @JvmStatic fun beforeClass() {
            MyDatabase.Schema.create(driver)
            dogQueries = database.dogQueries
        }

        @AfterClass @JvmStatic fun afterClass() {
            conn.close()
        }
    }
}
