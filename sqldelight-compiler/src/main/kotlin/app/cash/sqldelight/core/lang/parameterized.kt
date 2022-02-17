package app.cash.sqldelight.core.lang

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

internal val CURSOR_TYPE = ClassName("app.cash.sqldelight.db", "SqlCursor")
internal const val CURSOR_NAME = "cursor"

internal val PREPARED_STATEMENT_TYPE = ClassName("app.cash.sqldelight.db", "SqlPreparedStatement")

internal const val DRIVER_NAME = "driver"


private val DRIVER_TYPE = ClassName("app.cash.sqldelight.db", "SqlDriver")
internal val DATABASE_SCHEMA_TYPE = DRIVER_TYPE.nestedClass("Schema")

/**
 * Parameterize a `SqlDriver` by two type parameters: [statementType] and [cursorType].
 */
fun parameterizeSqlDriverBy(statementType: TypeName, cursorType: TypeName) = DRIVER_TYPE.parameterizedBy(
  statementType, cursorType
)

private val QUERY_TYPE = ClassName("app.cash.sqldelight", "Query")
internal val QUERY_LISTENER_TYPE = QUERY_TYPE.nestedClass("Listener")
internal val QUERY_LISTENER_LIST_TYPE = ClassName("kotlin.collections", "MutableList")
  .parameterizedBy(QUERY_LISTENER_TYPE)

/**
 * Parameterize a `Query` by two types: [dataType] and [cursorType].
 */
fun parameterizeQueryBy(dataType: TypeName, cursorType: TypeName) = QUERY_TYPE.parameterizedBy(
  dataType, cursorType
)

