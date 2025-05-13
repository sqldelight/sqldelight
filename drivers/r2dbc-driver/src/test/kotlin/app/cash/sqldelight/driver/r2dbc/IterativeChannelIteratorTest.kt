package app.cash.sqldelight.driver.r2dbc

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.test.runTest

@ExperimentalCoroutinesApi
class IterativeChannelIteratorTest {
  data class Value(
    var value: Int?,
  )

  @Test
  fun testAsSyncIteratorWithCleanup() = runTest {
    // R2DBC cleans values after onNext, so we do it here too.
    val publisher = flow {
      var counter = 0
      while (true) {
        val current = Value(counter)
        emit(current)
        current.value = null
        counter += 1
      }
    }.take(3).asPublisher()

    val iterator = publisher.asIterator()

    var lastValue: Value? = null
    while (true) {
      val current = iterator.next() ?: break
      assertNotNull(current.value)
      lastValue = current
    }
    assertNotNull(lastValue)
    assertNull(iterator.next())
  }
}
