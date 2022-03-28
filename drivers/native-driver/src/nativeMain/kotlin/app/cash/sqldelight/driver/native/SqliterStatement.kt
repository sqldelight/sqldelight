package app.cash.sqldelight.driver.native

import app.cash.sqldelight.db.SqlPreparedStatement
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.bindBlob
import co.touchlab.sqliter.bindDouble
import co.touchlab.sqliter.bindLong
import co.touchlab.sqliter.bindString

/**
 * @param [recycle] A function which recycles any resources this statement is backed by.
 */
internal class SqliterStatement(
  private val statement: Statement
) : SqlPreparedStatement {
  override fun bindBytes(index: Int, bytes: ByteArray?) {
    statement.bindBlob(index, bytes)
  }

  override fun bindLong(index: Int, long: Long?) {
    statement.bindLong(index, long)
  }

  override fun bindDouble(index: Int, double: Double?) {
    statement.bindDouble(index, double)
  }

  override fun bindString(index: Int, string: String?) {
    statement.bindString(index, string)
  }

  override fun bindBoolean(index: Int, boolean: Boolean?) {
    statement.bindLong(index, when (boolean) {
      null -> null
      true -> 1L
      false -> 0L
    })
  }
}
