package app.cash.sqldelight.postgresql.db

import app.cash.sqldelight.db.SqlPreparedStatement
import com.benasher44.uuid.Uuid
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

actual class PgSqlPreparedStatement : SqlPreparedStatement {

  actual fun bindUuid(index: Int, uuid: Uuid?) {
    TODO("Not yet implemented")
  }

  actual fun bindInstant(index: Int, instant: Instant?) {
    TODO("Not yet implemented")
  }

  actual fun bindLocalDateTime(index: Int, dateTime: LocalDateTime?) {
    TODO("Not yet implemented")
  }

  actual fun bindDate(index: Int, date: LocalDate?) {
    TODO("Not yet implemented")
  }

  actual fun bindDateTimePeriod(index: Int, period: DateTimePeriod?) {
    TODO("Not yet implemented")
  }

  override fun bindBytes(index: Int, bytes: ByteArray?) {
    TODO("Not yet implemented")
  }

  override fun bindLong(index: Int, long: Long?) {
    TODO("Not yet implemented")
  }

  override fun bindDouble(index: Int, double: Double?) {
    TODO("Not yet implemented")
  }

  override fun bindString(index: Int, string: String?) {
    TODO("Not yet implemented")
  }
}
