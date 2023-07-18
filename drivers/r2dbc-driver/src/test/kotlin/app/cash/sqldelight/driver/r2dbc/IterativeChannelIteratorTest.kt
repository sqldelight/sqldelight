package app.cash.sqldelight.driver.r2dbc

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
class IterativeChannelIteratorTest {
  data class Value(
    var value: Int?,
  )

  @Test
  fun testAsSyncIteratorWithCleanup() = runTest {
    val values = listOf(
      Value(0),
      Value(1),
      Value(2),
    )

    // R2DBC cleans values after onNext, so we do it here too.
    val publisher = flow {
      var counter = 0
      while(true) {
        val current = Value(counter)
        emit(current)
        current.value = null
        counter += 1
      }
    }.take(3).asPublisher()

    val iterator = publisher.iterator()

    var lastValue: Value? = null
    while (iterator.hasNext()) {
      println("while hasNext true")
      val current = iterator.next()
      println("while hasNext $current")
      assertNotNull(current.value)
      lastValue = current
    }
    assertNotNull(lastValue)
    assertFalse(iterator.hasNext())
  }
}
