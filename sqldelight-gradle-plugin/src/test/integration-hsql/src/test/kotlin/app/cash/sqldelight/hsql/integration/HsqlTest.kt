package app.cash.sqldelight.hsql.integration

import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import com.google.common.truth.Truth.assertThat
import java.sql.Connection
import java.sql.DriverManager
import org.junit.After
import org.junit.Before
import org.junit.Test

class HsqlTest {
  val conn = DriverManager.getConnection("jdbc:hsqldb:mem:mymemdb;shutdown=true")
  val driver = object : JdbcDriver() {
    override fun getConnection() = conn
    override fun closeConnection(connection: Connection) = Unit
    override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun notifyListeners(vararg queryKeys: String) = Unit
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

  @Test fun charLengthFunctionReturnsCharacterCount() {
    database.charactersQueries.insertCharacter("abcdef", null)
    val length = database.charactersQueries.selectNameCharLength().executeAsOne()
    assertThat(length).isEqualTo(6)
    val nullLength = database.charactersQueries.selectDescriptionCharLength().executeAsOne()
    assertThat(nullLength.char_length).isNull()
  }
}
