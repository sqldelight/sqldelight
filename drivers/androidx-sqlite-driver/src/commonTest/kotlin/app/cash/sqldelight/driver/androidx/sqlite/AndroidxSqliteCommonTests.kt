package app.cash.sqldelight.driver.androidx.sqlite

import androidx.sqlite.SQLiteDriver
import app.cash.sqldelight.Transacter
import com.squareup.sqldelight.driver.test.DriverTest
import com.squareup.sqldelight.driver.test.QueryTest
import com.squareup.sqldelight.driver.test.TransacterTest

expect abstract class CommonDriverTest() : DriverTest
expect abstract class CommonQueryTest() : QueryTest
expect abstract class CommonTransacterTest() : TransacterTest

expect fun androidxSqliteTestDriver(): SQLiteDriver

expect inline fun <T> assertChecksThreadConfinement(
  transacter: Transacter,
  crossinline scope: Transacter.(T.() -> Unit) -> Unit,
  crossinline block: T.() -> Unit,
)
