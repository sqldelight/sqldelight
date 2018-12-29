package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.NativeFileContext.deleteDatabase
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.driver.test.TransacterTest
import kotlin.test.BeforeTest
import kotlin.native.concurrent.freeze

class IosTransacterTest: TransacterTest() {
  override fun setupDatabase(schema: SqlDatabase.Schema): SqlDatabase {
    val name = "testdb"
    deleteDatabase(name)
    return NativeSqlDatabase(schema, name)
  }

  @BeforeTest fun setup2() {
    super.setup()
    transacter.freeze()
  }
}