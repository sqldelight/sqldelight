package app.cash.sqldelight.driver.r2dbc

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class IterativeChannelIteratorTest {
  data class Value(
    var value: Long?,
  )

  // R2DBC cleans values after onComplete, so we do it here too.
  private fun Flow<Value>.withCleanup(): Flow<Value> = flow {
    var last: Value? = null
    collect {
      val lastValue = last
      if(lastValue != null) {
        lastValue.value = null
      }
      last = it
      emit(it)
    }
  }
  
  @Test
  fun testAsSyncIteratorWithCleanup() = runTest(dispatchTimeoutMs = 3000) {
      val values = listOf(
        Value(0),
        Value(1),
        Value(2),
      )

      val publisher = values.asFlow().withCleanup()
      val state = publisher.iterator()

      var lastValue: Value? = null 
      repeat(3) {
        val next = assertNotNull(state.next())
        assertNotNull(next.value)
        lastValue = next
      }
      assertNull(state.next())
      val lastValueNN = assertNotNull(lastValue)
      assertNull(lastValueNN.value)
    }
}
