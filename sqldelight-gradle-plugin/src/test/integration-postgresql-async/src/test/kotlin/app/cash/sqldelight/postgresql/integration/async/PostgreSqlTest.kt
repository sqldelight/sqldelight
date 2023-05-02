package app.cash.sqldelight.postgresql.integration.async

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.r2dbc.R2dbcDriver
import com.google.common.truth.Truth.assertThat
import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.reactive.awaitSingle
import org.junit.Assert
import org.junit.Test

class PostgreSqlTest {
  private val factory = ConnectionFactories.get("r2dbc:tc:postgresql:///myDb?TC_IMAGE_TAG=9.6.8")

  private fun runTest(block: suspend (MyDatabase) -> Unit) = kotlinx.coroutines.test.runTest {
    val connection = factory.create().awaitSingle()
    val awaitClose = CompletableDeferred<Unit>()
    val driver = R2dbcDriver(connection, backgroundScope.coroutineContext) {
      awaitClose.complete(Unit)
    }
    val db = MyDatabase(driver)
    MyDatabase.Schema.awaitCreate(driver)
    block(db)
    driver.close()
    awaitClose.await()
  }

  @Test fun simpleSelectWithNullPrimitive() = runTest { database ->
    Assert.assertEquals(0, database.dogQueries.selectDogs().awaitAsList().size)
    database.dogQueries.insertDog("Tilda", "Pomeranian", null)
    assertThat(database.dogQueries.selectDogs().awaitAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = 1,
          age = null,
        ),
      )
  }
}
