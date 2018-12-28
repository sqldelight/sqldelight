package com.squareup.sqldelight.drivers.ios

import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.Statement
import co.touchlab.sqliter.bindBlob
import co.touchlab.sqliter.bindDouble
import co.touchlab.sqliter.bindLong
import co.touchlab.sqliter.bindString
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlPreparedStatement

/**
 * @param [recycle] A function which recycles any resources this statement is backed by.
 */
internal class SqliterStatement(
  private val statement: Statement,
  private val track: (Cursor) -> Unit,
  private val recycle: (Statement) -> Unit
) : SqlPreparedStatement {
  override fun bindBytes(index: Int, value: ByteArray?) {
    statement.bindBlob(index, value)
  }

  override fun bindLong(index: Int, value: Long?) {
    statement.bindLong(index, value)
  }

  override fun bindDouble(index: Int, value: Double?) {
    statement.bindDouble(index, value)
  }

  override fun bindString(index: Int, value: String?) {
    statement.bindString(index, value)
  }

  override fun executeQuery(): SqlCursor {
    val cursor = statement.query()
    track(cursor)
    return SqliterSqlCursor(cursor, { recycle(statement) })
  }

  override fun execute() {
    statement.execute()
    statement.resetStatement()
    recycle(statement)
  }
}