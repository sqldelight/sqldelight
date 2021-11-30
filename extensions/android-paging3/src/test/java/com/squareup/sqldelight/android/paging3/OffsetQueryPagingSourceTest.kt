/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.android.paging3

import androidx.paging.PagingSource.LoadParams.Refresh
import androidx.paging.PagingSource.LoadResult
import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
class OffsetQueryPagingSourceTest {

  private lateinit var driver: SqlDriver
  private lateinit var transacter: Transacter

  @Before fun before() {
    driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    driver.execute(null, "CREATE TABLE testTable(value INTEGER PRIMARY KEY)", 0)
    (0L until 10L).forEach(this::insert)
    transacter = object : TransacterImpl(driver) {}
  }

  @Test fun `empty page gives correct prevKey and nextKey`() {
    driver.execute(null, "DELETE FROM testTable", 0)
    val source = OffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    val results = runBlocking { source.load(Refresh(null, 2, false)) }

    assertNull((results as LoadResult.Page).prevKey)
    assertNull(results.nextKey)
  }

  @Test fun `aligned first page gives correct prevKey and nextKey`() {
    val source = OffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    val results = runBlocking { source.load(Refresh(null, 2, false)) }

    assertNull((results as LoadResult.Page).prevKey)
    assertEquals(2L, results.nextKey)
  }

  @Test fun `aligned last page gives correct prevKey and nextKey`() {
    val source = OffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    val results = runBlocking { source.load(Refresh(8, 2, false)) }

    assertEquals(6L, (results as LoadResult.Page).prevKey)
    assertNull(results.nextKey)
  }

  @Test fun `simple sequential page exhaustion gives correct results`() {
    val source = OffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    runBlocking {
      val expected = (0L until 10L).chunked(2).iterator()
      var nextKey: Long? = null
      do {
        val results = source.load(Refresh(nextKey, 2, false))
        assertEquals(expected.next(), (results as LoadResult.Page).data)
        nextKey = results.nextKey
      } while (nextKey != null)
    }
  }

  @Test fun `misaligned refresh at end page boundary gives null nextKey`() {
    val source = OffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    val results = runBlocking { source.load(Refresh(9, 2, false)) }

    assertEquals(7L, (results as LoadResult.Page).prevKey)
    assertNull(results.nextKey)
  }

  @Test fun `misaligned refresh at first page boundary gives proper prevKey`() {
    val source = OffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    val results = runBlocking { source.load(Refresh(1L, 2, false)) }

    assertEquals(-1L, (results as LoadResult.Page).prevKey)
    assertEquals(3L, results.nextKey)
  }

  @Test fun `initial page has correct itemsBefore and itemsAfter`() {
    val source = OffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    val results = runBlocking { source.load(Refresh(null, 2, false)) }

    assertEquals(0, (results as LoadResult.Page).itemsBefore)
    assertEquals(8, results.itemsAfter)
  }

  @Test fun `middle page has correct itemsBefore and itemsAfter`() {
    val source = OffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    val results = runBlocking { source.load(Refresh(4, 2, false)) }

    assertEquals(4, (results as LoadResult.Page).itemsBefore)
    assertEquals(4, results.itemsAfter)
  }

  @Test fun `end page has correct itemsBefore and itemsAfter`() {
    val source = OffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    val results = runBlocking { source.load(Refresh(8, 2, false)) }

    assertEquals(8, (results as LoadResult.Page).itemsBefore)
    assertEquals(0, results.itemsAfter)
  }

  @Test fun `misaligned end page has correct itemsBefore and itemsAfter`() {
    val source = OffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    val results = runBlocking { source.load(Refresh(9, 2, false)) }

    assertEquals(9, (results as LoadResult.Page).itemsBefore)
    assertEquals(0, results.itemsAfter)
  }

  @Test fun `misaligned start page has correct itemsBefore and itemsAfter`() {
    val source = OffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    val results = runBlocking { source.load(Refresh(1, 2, false)) }

    assertEquals(1, (results as LoadResult.Page).itemsBefore)
    assertEquals(7, results.itemsAfter)
  }

  @Test fun `prepend paging misaligned start page produces correct values`() {
    val source = OffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    runBlocking {
      val expected = listOf(listOf(1L, 2L), listOf(0L)).iterator()
      var prevKey: Long? = 1L
      do {
        val results = source.load(Refresh(prevKey, 2, false))
        assertEquals(expected.next(), (results as LoadResult.Page).data)
        prevKey = results.prevKey
      } while (prevKey != null)
    }
  }

  @Test fun `key too big throws IndexOutOfBoundsException`() {
    val source = OffsetQueryPagingSource(
      this::query,
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    runBlocking {
      assertFailsWith<IndexOutOfBoundsException> {
        source.load(Refresh(10, 2, false))
      }
    }
  }

  @Test fun `query invalidation invalidates paging source`() {
    val query = query(2, 0)
    val source = OffsetQueryPagingSource(
      { _, _ -> query },
      countQuery(),
      transacter,
      TestCoroutineDispatcher()
    )

    runBlocking { source.load(Refresh(null, 0, false)) }

    driver.notifyListeners(arrayOf("testTable"))

    assertTrue(source.invalid)
  }

  private fun query(limit: Long, offset: Long) = object : Query<Long>(
    { cursor -> cursor.getLong(0)!! }
  ) {
    override fun execute() = driver.executeQuery(1, "SELECT value FROM testTable LIMIT ? OFFSET ?", 2) {
      bindLong(1, limit)
      bindLong(2, offset)
    }

    override fun addListener(listener: Listener) = driver.addListener(listener, arrayOf("testTable"))
    override fun removeListener(listener: Listener) = driver.removeListener(listener, arrayOf("testTable"))
  }

  private fun countQuery() = Query(
    2,
    arrayOf("testTable"),
    driver,
    "Test.sq",
    "count", "SELECT count(*) FROM testTable",
    { it.getLong(0)!! }
  )

  private fun insert(value: Long, db: SqlDriver = driver) {
    db.execute(0, "INSERT INTO testTable (value) VALUES (?)", 1) {
      bindLong(1, value)
    }
  }
}
