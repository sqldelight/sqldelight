package app.cash.sqldelight.driver.androidx.sqlite

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.AndroidSQLiteDriver
import app.cash.sqldelight.Transacter
import com.squareup.sqldelight.driver.test.DriverTest
import com.squareup.sqldelight.driver.test.QueryTest
import com.squareup.sqldelight.driver.test.TransacterTest
import java.util.concurrent.Semaphore
import org.junit.Assert
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
actual abstract class CommonDriverTest : DriverTest()

@RunWith(RobolectricTestRunner::class)
actual abstract class CommonQueryTest : QueryTest()

@RunWith(RobolectricTestRunner::class)
actual abstract class CommonTransacterTest : TransacterTest()

actual fun androidxSqliteTestDriver(): SQLiteDriver = AndroidSQLiteDriver()

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
