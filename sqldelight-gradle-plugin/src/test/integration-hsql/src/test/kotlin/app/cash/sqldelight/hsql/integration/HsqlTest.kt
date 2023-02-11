package app.cash.sqldelight.hsql.integration

import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

class HsqlTest {
  val conn = DriverManager.getConnection("jdbc:hsqldb:mem:mymemdb")
  val driver = object : JdbcDriver() {
    override fun getConnection() = conn
    override fun closeConnection(connection: Connection) = Unit
    override fun addListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
    override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) = Unit
    override fun notifyListeners(queryKeys: Array<String>) = Unit
  }
  val database = MyDatabase(driver)

  @Before fun before() {
    MyDatabase.Schema.create(driver)
  }

  @After fun after() {
    conn.close()
  }

  @Test fun simpleSelect() {
    database.dogQueries.insertDog("Tilda", "Pomeranian", 1)
    assertThat(database.dogQueries.selectDogs().executeAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = true,
          id = 1,
          id_bigger_than_ten = false,
        ),
      )
  }

  @Test fun selectWhereNullable() {
    database.nullableValueQueries.insertValue("abc")
    database.nullableValueQueries.insertValue(null)

    assertThat(database.nullableValueQueries.selectValue("abc").executeAsOne())
      .isEqualTo(
        NullableValue("abc"),
      )
    assertThat(database.nullableValueQueries.selectValue(null).executeAsOne())
      .isEqualTo(
        NullableValue(null),
      )
  }
}
