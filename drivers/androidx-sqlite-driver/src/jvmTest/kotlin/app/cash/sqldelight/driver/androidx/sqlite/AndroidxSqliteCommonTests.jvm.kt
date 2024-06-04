package app.cash.sqldelight.driver.androidx.sqlite

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.Transacter
import com.squareup.sqldelight.driver.test.DriverTest
import com.squareup.sqldelight.driver.test.QueryTest
import com.squareup.sqldelight.driver.test.TransacterTest
import java.util.concurrent.Semaphore
import org.junit.Assert

actual abstract class CommonDriverTest : DriverTest()
actual abstract class CommonQueryTest : QueryTest()
actual abstract class CommonTransacterTest : TransacterTest()

actual fun androidxSqliteTestDriver(): SQLiteDriver = BundledSQLiteDriver()

actual inline fun <T> assertChecksThreadConfinement(
  transacter: Transacter,
  crossinline scope: Transacter.(T.() -> Unit) -> Unit,
  crossinline block: T.() -> Unit,
) {
  lateinit var thread: Thread
  var result: Result<Unit>? = null
  val semaphore = Semaphore(0)

  transacter.scope {
    thread = kotlin.concurrent.thread {
      result = runCatching {
        this@scope.block()
      }

      semaphore.release()
    }
  }

  semaphore.acquire()
  thread.interrupt()
  Assert.assertThrows(IllegalStateException::class.java) {
    result!!.getOrThrow()
  }
}
