package com.squareup.sqldelight.integration

import com.example.Database
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver.Companion.IN_MEMORY
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.Query
import org.junit.Before
import org.junit.Test

import com.google.common.truth.Truth.assertThat

class IntegrationTests {
  private lateinit var database: Database

  @Before fun before() {
    val driver = JdbcSqliteDriver(IN_MEMORY)
    Database.Schema.create(driver)

    database = Database(driver)
  }

  @Test fun insertInSubmodule() {
    var timesNotified = 0
    val selectData = database.otherQueries.selectData()

    selectData.addListener(object : Query.Listener {
      override fun queryResultsChanged() {
        timesNotified++
      }
    })

    assertThat(selectData.executeAsList()).containsExactly("first_value")

    database.dataQueries.insert("second_value")

    assertThat(timesNotified).isEqualTo(1)
    assertThat(selectData.executeAsList()).containsExactly("first_value", "second_value").inOrder()
  }
}
