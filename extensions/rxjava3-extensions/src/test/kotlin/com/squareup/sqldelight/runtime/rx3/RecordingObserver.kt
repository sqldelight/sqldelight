/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.sqldelight.runtime.rx3

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.Query
import io.reactivex.rxjava3.observers.DisposableObserver
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque

internal class RecordingObserver(val numberOfColumns: Int) : DisposableObserver<Query<*>>() {

  val events: BlockingDeque<Any> = LinkedBlockingDeque()

  override fun onComplete() {
    events.add(COMPLETED)
  }

  override fun onError(e: Throwable) {
    events.add(e)
  }

  override fun onNext(value: Query<*>) {
    val allRows = value.execute { cursor ->
      val data = mutableListOf<List<String?>>()
      while (cursor.next())
        data.add((0 until numberOfColumns).map(cursor::getString))
      data
    }
    events.add(allRows)
  }

  fun takeEvent(): Any {
    return events.removeFirst() ?: throw AssertionError("No items.")
  }

  @Suppress("UNCHECKED_CAST")
  fun assertResultSet(): ResultSetAssert {
    val event = takeEvent()
    assertThat(event).isInstanceOf(List::class.java)
    return ResultSetAssert(event as List<List<String?>>)
  }

  fun assertErrorContains(expected: String) {
    val event = takeEvent()
    assertThat(event).isInstanceOf(Throwable::class.java)
    assertThat((event as Throwable).message).contains(expected)
  }

  fun assertIsCompleted() {
    val event = takeEvent()
    assertThat(event).isEqualTo(COMPLETED)
  }

  fun assertNoMoreEvents() {
    assertThat(events).isEmpty()
  }

  internal class ResultSetAssert(private val rows: List<List<String?>>) {
    private var row = 0

    fun hasRow(vararg values: String?): ResultSetAssert {
      assertThat(row < rows.count()).named("row ${row + 1} exists").isTrue()
      val thisRow = rows[row]
      row += 1
      for (i in values.indices) {
        assertThat(thisRow[i])
          .named("row $row column '$i'")
          .isEqualTo(values[i])
      }
      return this
    }

    fun isExhausted() {
      if (row < rows.count()) {
        throw AssertionError("Expected no more rows but was")
      }
    }
  }

  companion object {
    private const val COMPLETED = "<completed>"
  }
}
