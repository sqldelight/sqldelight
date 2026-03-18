package app.cash.sqldelight.paging3

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
fun DbTest.runDbTest(body: suspend TestScope.() -> Unit) = runTest(timeout = 1000.seconds) {
  val driver = provideDbDriver()
  setup(driver)
  body()
  driver.close()
}
