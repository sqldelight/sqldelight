package app.cash.sqldelight.db

import com.benasher44.uuid.Uuid
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Represents a PostgreSQL result set which can be iterated through with [next]. Initially the cursor will
 * not point to any row, and calling [next] once will iterate to the first row.
 */
interface PgSqlCursor : SqlCursor {

  /**
   * Get a [Uuid] from the underlying statement at [index].
   */
  fun getUuid(index: Int): Uuid?

  /**
   * Get a [Instant] from the underlying statement at [index].
   */
  fun getInstant(index: Int): Instant?

  /**
   * Get a [LocalDateTime] from the underlying statement at [index].
   */
  fun getLocalDateTime(index: Int): LocalDateTime?

  /**
   * Get a [LocalDate] from the underlying statement at [index].
   */
  fun getDate(index: Int): LocalDate?

  /**
   * Get a [DateTimePeriod] from the underlying statement at [index].
   */
  fun getDateTimePeriod(index: Int): DateTimePeriod?
}
