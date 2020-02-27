package com.squareup.sqldelight.jdbc.driver

import com.mysql.cj.jdbc.MysqlDataSource
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlDriver.Schema
import com.squareup.sqldelight.driver.test.TransacterTest

class JdbcTransacterTest : TransacterTest() {
  override fun setupDatabase(schema: Schema): SqlDriver {
    val dataSource = MysqlDataSource().apply {
      setUrl("jdbc:mysql://localhost:3306?user=root&password=root")
    }
    val database = JdbcDriver(dataSource)
    schema.create(database)
    return database
  }
}
