package app.cash.sqldelight.dialects.postgresql

import app.cash.sqldelight.dialect.api.DialectType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName

internal enum class PostgreSqlType(override val javaType: TypeName) : DialectType {
  SMALL_INT(SHORT),
  INTEGER(INT),
  BIG_INT(LONG),
  DATE(ClassName("java.time", "LocalDate")),
  TIME(ClassName("java.time", "LocalTime")),
  TIMESTAMP(ClassName("java.time", "LocalDateTime")),
  TIMESTAMP_TIMEZONE(ClassName("java.time", "OffsetDateTime")),
  INTERVAL(ClassName("org.postgresql.util", "PGInterval")),
  UUID(ClassName("java.util", "UUID")),
  NUMERIC(ClassName("java.math", "BigDecimal")),
  JSON(STRING),
  TSVECTOR(STRING),
  ;

  override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
    return when (this) {
      SMALL_INT -> CodeBlock.of("bindShort(%L, %L)\n", columnIndex, value)
      INTEGER -> CodeBlock.of("bindInt(%L, %L)\n", columnIndex, value)
      BIG_INT -> CodeBlock.of("bindLong(%L, %L)\n", columnIndex, value)
      DATE, TIME, TIMESTAMP, TIMESTAMP_TIMEZONE, INTERVAL, UUID -> CodeBlock.of(
        "bindObject(%L, %L)\n",
        columnIndex,
        value,
      )

      NUMERIC -> CodeBlock.of("bindBigDecimal(%L, %L)\n", columnIndex, value)
      JSON, TSVECTOR -> CodeBlock.of(
        "bindObject(%L, %L, %M)\n",
        columnIndex,
        value,
        MemberName(ClassName("java.sql", "Types"), "OTHER"),
      )
    }
  }

  override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
    return CodeBlock.of(
      when (this) {
        SMALL_INT -> "$cursorName.getShort($columnIndex)"
        INTEGER -> "$cursorName.getInt($columnIndex)"
        BIG_INT -> "$cursorName.getLong($columnIndex)"
        DATE, TIME, TIMESTAMP, TIMESTAMP_TIMEZONE, INTERVAL, UUID -> "$cursorName.getObject<%T>($columnIndex)"
        NUMERIC -> "$cursorName.getBigDecimal($columnIndex)"
        JSON, TSVECTOR -> "$cursorName.getString($columnIndex)"
      },
      javaType,
    )
  }
}
