package app.cash.sqldelight.postgresql.integration

import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import com.google.common.truth.Truth.assertThat
import migrations.app.cash.sqldelight.postgresql.integration.Orders
import migrations.app.cash.sqldelight.postgresql.integration.Products
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

class PostgreSqlTest {
  val conn = DriverManager.getConnection("jdbc:tc:postgresql:13.11:///my_db")
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

  @Test fun simpleProductsSelect() {
    with(database) {
      productsQueries.insert(Products(1, "sku", true))
      assertThat(productsQueries.selectAll().executeAsList()).containsExactly(
        Products(
          1,
          "sku",
          true,
        ),
      )
    }
  }

  @Test fun simpleOrdersSelect() {
    with(database) {
      ordersQueries.insert(Orders(1, "sku", 3, 165.98.toBigDecimal()))
      assertThat(ordersQueries.selectAll().executeAsList()).containsExactly(
        Orders(
          1,
          "sku",
          3,
          165.98.toBigDecimal(),
        ),
      )
    }
  }
}
