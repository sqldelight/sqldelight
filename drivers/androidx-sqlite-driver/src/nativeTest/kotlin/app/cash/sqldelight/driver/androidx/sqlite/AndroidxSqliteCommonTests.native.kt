package app.cash.sqldelight.driver.androidx.sqlite

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.Transacter
import com.squareup.sqldelight.driver.test.DriverTest
import com.squareup.sqldelight.driver.test.QueryTest
import com.squareup.sqldelight.driver.test.TransacterTest
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicReference
import kotlin.native.concurrent.Worker
import kotlin.test.assertFailsWith

actual abstract class CommonDriverTest : DriverTest()
actual abstract class CommonQueryTest : QueryTest()
actual abstract class CommonTransacterTest : TransacterTest()

actual fun androidxSqliteTestDriver(): SQLiteDriver = BundledSQLiteDriver()

actual inline fun <T> assertChecksThreadConfinement(
  transacter: Transacter,
  crossinline scope: Transacter.(T.() -> Unit) -> Unit,
  crossinline block: T.() -> Unit,
) {
  val resultRef = AtomicReference<Result<Unit>?>(null)
  val semaphore = AtomicInt(0)

  transacter.scope {
    val worker = Worker.start()
    worker.executeAfter(0L) {
      resultRef.value = runCatching {
        this@scope.block()
      }
      semaphore.value = 1
    }
    worker.requestTermination()
  }

  while (semaphore.value == 0) {
    Worker.current.processQueue()
  }

  assertFailsWith<IllegalStateException> {
    resultRef.value!!.getOrThrow()
  }
}
