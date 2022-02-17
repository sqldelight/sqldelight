package app.cash.sqldelight.core.lang

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

internal val CURSOR_TYPE = ClassName("app.cash.sqldelight.db", "SqlCursor")
internal const val CURSOR_NAME = "cursor"

internal val PREPARED_STATEMENT_TYPE = ClassName("app.cash.sqldelight.db", "SqlPreparedStatement")

private val DRIVER_TYPE = ClassName("app.cash.sqldelight.db", "SqlDriver")
internal const val DRIVER_NAME = "driver"

internal val DATABASE_SCHEMA_TYPE = DRIVER_TYPE.nestedClass("Schema")

fun parameterizeSqlDriverBy(statementType: TypeName, cursorType: TypeName) = DRIVER_TYPE.parameterizedBy(
  statementType, cursorType
)