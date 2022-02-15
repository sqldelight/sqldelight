package app.cash.sqldelight.db

import com.benasher44.uuid.Uuid
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimePeriod

/**
 * A PostgreSQL specific extension to [SqlPreparedStatement]. This interface offers
 * bindings for [Uuid] and a myriad of datetime values.
 */
interface PgSqlPreparedStatement : SqlPreparedStatement {

  /**
   * Bind [uuid] to the underlying statement at [index] as the PostgreSQL `UUID` type.
   */
  fun bindUuid(index: Int, uuid: Uuid?)

  /**
   * Bind [instant] to the underlying statement at [index] as the PostgreSQL `TIMESTAMP WITHOUT TIMEZONE` type.
   */
  fun bindInstant(index: Int, instant: Instant?)

  /**
   * Bind [dateTime] to the underlying statement at [index] as the PostgreSQL `TIMESTAMP WITH TIMEZONE` type.
   */
  fun bindLocalDateTime(index: Int, dateTime: LocalDateTime?)

  /**
   * Bind [date] to the underlying statement at [index] as the PostgreSQL `DATE` type.
   */
  fun bindDate(index: Int, date: LocalDate?)

  /**
   * Bind [period] to the underlying statement at [index] as the PostgreSQL `INTERVAL` type.
   */
  fun bindDateTimePeriod(index: Int, period: DateTimePeriod?)
}