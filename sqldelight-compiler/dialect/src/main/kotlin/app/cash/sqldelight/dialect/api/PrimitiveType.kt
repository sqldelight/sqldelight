package app.cash.sqldelight.dialect.api

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

/**
 * Types which are retrieved the same way for all dialects.
 */
enum class PrimitiveType(override val javaType: TypeName) : DialectType {
  ARGUMENT(ANY.copy(nullable = true)),
  NULL(Nothing::class.asClassName().copy(nullable = true)),
  INTEGER(LONG),
  REAL(DOUBLE),
  TEXT(String::class.asTypeName()),
  BLOB(ByteArray::class.asTypeName());

  override fun prepareStatementBinder(columnIndex: String, value: CodeBlock): CodeBlock {
    return CodeBlock.builder()
      .add(
        when (this) {
          INTEGER -> "bindLong"
          REAL -> "bindDouble"
          TEXT -> "bindString"
          BLOB -> "bindBytes"
          else -> throw IllegalArgumentException("Cannot bind unknown types or null")
        }
      )
      .add("($columnIndex, %L)\n", value)
      .build()
  }

  override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
    return CodeBlock.of(
      when (this) {
        NULL -> "null"
        INTEGER -> "$cursorName.getLong($columnIndex)"
        REAL -> "$cursorName.getDouble($columnIndex)"
        TEXT -> "$cursorName.getString($columnIndex)"
        BLOB -> "$cursorName.getBytes($columnIndex)"
        ARGUMENT -> throw IllegalArgumentException("Cannot retrieve argument from cursor")
      }
    )
  }
}
