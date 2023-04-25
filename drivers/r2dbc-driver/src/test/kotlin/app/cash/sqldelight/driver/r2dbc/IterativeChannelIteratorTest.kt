package app.cash.sqldelight.driver.r2dbc

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class IterativeChannelIteratorTest {
  data class Value(
    var value: Long?,
  )

  @Test
  fun testAsSyncIteratorWithCleanup() = runTest {
    val values = listOf(
      Value(0),
      Value(1),
      Value(2),
    )

    // R2DBC cleans values after onComplete, so we do it here too.
    fun cleanup() {
      for (value in values) {
        value.value = null
      }
    }

    val publisher = values.asFlow().onCompletion {
      cleanup()
    }.asPublisher()

    val iterator = publisher.iterator()

    var lastValue: Value? = null
    while (iterator.hasNext()) {
      val current = iterator.next()
      assertNotNull(current.value)
      lastValue = current
    }
    assertNotNull(lastValue)
    assertFalse(iterator.hasNext())
    assertNull(lastValue.value)
  }
}
