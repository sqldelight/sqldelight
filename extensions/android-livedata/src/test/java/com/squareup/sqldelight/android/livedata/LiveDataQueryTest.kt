package com.squareup.sqldelight.android.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
class LiveDataQueryTest {
  private lateinit var driver: SqlDriver

  @Before
  fun before() {
    driver = AndroidSqliteDriver(object : SqlDriver.Schema {
      override val version: Int = 1

      override fun create(driver: SqlDriver) {
        driver.execute(null, "CREATE TABLE testTable (value INTEGER PRIMARY KEY)", 0)

        for (i in 0L..100L) {
          insert(i, driver)
        }
      }

      override fun migrate(
        driver: SqlDriver,
        oldVersion: Int,
        newVersion: Int
      ) {
        throw AssertionError("DB Migration shouldn't occur")
      }
    }, RuntimeEnvironment.application)
  }

  @Test
  fun mapToOne() {
    createQuery("value=1")
      .mapToOne(Executor { it.run() })
      .assertResult(1)
  }

  @Test
  fun mapToOneOrNull() {
    createQuery("value=1")
      .mapToOneOrNull(Executor { it.run() })
      .assertResult(1)
  }

  @Test
  fun `mapToOneOrNull with multiple rows only first handled`() {
    createQuery("value<=2 ORDER BY value ASC")
      .mapToOneOrNull(Executor { it.run() })
      .assertResult(0)
  }

  @Test
  fun `mapToOneOrNull with no matches`() {
    createQuery("value=101")
      .mapToOneOrNull(Executor { it.run() })
      .assertResult(null)
  }

  @Test
  fun mapToList() {
    createQuery("value<=2 ORDER BY value ASC")
      .mapToList(Executor { it.run() })
      .assertResult(listOf(0L, 1L, 2L))
  }

  @Test
  fun `mapToList empty when no rows`() {
    createQuery("value<=2 LIMIT 0")
      .mapToList(Executor { it.run() })
      .assertResult(emptyList())
  }

  private fun insert(value: Long, db: SqlDriver = driver) {
    db.execute(0, "INSERT INTO testTable (value) VALUES (?)", 1) {
      bindLong(1, value)
    }
  }

  private fun createQuery(whereStatement: String): LiveData<Query<Long>> {
    return object : Query<Long>(
      mutableListOf(),
      { cursor -> cursor.getLong(0)!! }
    ) {
      override fun execute() = driver.executeQuery(1, "SELECT value FROM testTable WHERE $whereStatement", 0)
    }.asLiveData()
  }

  private fun <T : Any> LiveData<T>.assertResult(result: T?) {
    val latch = CountDownLatch(1)
    observeForever(object : Observer<T> {
      override fun onChanged(t: T?) {
        assertThat(t).isEqualTo(result)
        latch.countDown()
        removeObserver(this)
      }
    })
    latch.await()
  }
}
