package app.cash.sqldelight.integration

import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import com.example.android.Database
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

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
