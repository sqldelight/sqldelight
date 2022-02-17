package app.cash.sqldelight.postgresql.db

import app.cash.sqldelight.db.SqlCursor
import com.benasher44.uuid.Uuid
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

actual class PgSqlCursor : SqlCursor {

  override fun next(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getString(index: Int): String? {
    TODO("Not yet implemented")
  }

  override fun getLong(index: Int): Long? {
    TODO("Not yet implemented")
  }

  override fun getBytes(index: Int): ByteArray? {
    TODO("Not yet implemented")
  }

  override fun getDouble(index: Int): Double? {
    TODO("Not yet implemented")
  }

  actual fun getUuid(index: Int): Uuid? {
    TODO()
  }

  actual fun getInstant(index: Int): Instant? {
    TODO("Not yet implemented")
  }

  actual fun getLocalDateTime(index: Int): LocalDateTime? {
    TODO("Not yet implemented")
  }

  actual fun getDate(index: Int): LocalDate? {
    TODO("Not yet implemented")
  }

  actual fun getDateTimePeriod(index: Int): DateTimePeriod? {
    TODO("Not yet implemented")
  }

  actual override fun close() {
    TODO("Not yet implemented")
  }
}
