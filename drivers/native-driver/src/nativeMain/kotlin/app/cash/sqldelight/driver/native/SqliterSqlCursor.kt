package app.cash.sqldelight.driver.native

import app.cash.sqldelight.db.SqlCursor
import co.touchlab.sqliter.Cursor
import co.touchlab.sqliter.getBytesOrNull
import co.touchlab.sqliter.getDoubleOrNull
import co.touchlab.sqliter.getLongOrNull
import co.touchlab.sqliter.getStringOrNull

/**
 * Wrapper for cursor calls. Cursors point to real SQLite statements, so we need to be careful with
 * them. If dev closes the outer structure, this will get closed as well, which means it could start
 * throwing errors if you're trying to access it.
 */
internal class SqliterSqlCursor(private val cursor: Cursor) : SqlCursor {
  override fun getBytes(index: Int): ByteArray? = cursor.getBytesOrNull(index)

  override fun getDouble(index: Int): Double? = cursor.getDoubleOrNull(index)

  override fun getLong(index: Int): Long? = cursor.getLongOrNull(index)

  override fun getString(index: Int): String? = cursor.getStringOrNull(index)

  override fun getBoolean(index: Int): Boolean? {
    return (cursor.getLongOrNull(index) ?: return null) == 1L
  }

  override fun next(): Boolean = cursor.next()
}
