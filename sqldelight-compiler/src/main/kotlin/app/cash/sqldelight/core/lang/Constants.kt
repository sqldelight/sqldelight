package app.cash.sqldelight.core.lang

import com.intellij.openapi.vfs.VirtualFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal val CURSOR_TYPE = ClassName("app.cash.sqldelight.db", "SqlCursor")
internal const val CURSOR_NAME = "cursor"

internal val DRIVER_TYPE = ClassName("app.cash.sqldelight.db", "SqlDriver")
internal val ASYNC_DRIVER_TYPE = ClassName("app.cash.sqldelight.async.db", "AsyncSqlDriver")
internal const val DRIVER_NAME = "driver"
internal val DATABASE_SCHEMA_TYPE = DRIVER_TYPE.nestedClass("Schema")
internal val ASYNC_DATABASE_SCHEMA_TYPE = ASYNC_DRIVER_TYPE.nestedClass("Schema")

internal val PREPARED_STATEMENT_TYPE = ClassName("app.cash.sqldelight.db", "SqlPreparedStatement")

internal const val CUSTOM_DATABASE_NAME = "database"

internal const val ADAPTER_NAME = "Adapter"

internal val QUERY_TYPE = ClassName("app.cash.sqldelight", "Query")
internal val ASYNC_QUERY_TYPE = ClassName("app.cash.sqldelight.async", "AsyncQuery")
internal val EXECUTABLE_QUERY_TYPE = ClassName("app.cash.sqldelight", "ExecutableQuery")
internal val ASYNC_EXECUTABLE_QUERY_TYPE = ClassName("app.cash.sqldelight.async", "AsyncExecutableQuery")
internal val QUERY_LISTENER_TYPE = QUERY_TYPE.nestedClass("Listener")
internal val QUERY_LISTENER_LIST_TYPE = ClassName("kotlin.collections", "MutableList")
  .parameterizedBy(QUERY_LISTENER_TYPE)

internal val ASYNC_DRIVER_CALLBACK_TYPE = ASYNC_DRIVER_TYPE.nestedClass("Callback")

internal const val CALLBACK_SUCCESS_NAME = "onSuccess"
internal const val CALLBACK_ERROR_NAME = "onError"

internal const val MAPPER_NAME = "mapper"
internal const val EXECUTE_BLOCK_NAME = "block"

internal const val EXECUTE_METHOD = "execute"

const val QUERIES_SUFFIX_NAME = "Queries"

val VirtualFile.queriesName
  get() = "${nameWithoutExtension.capitalize()}$QUERIES_SUFFIX_NAME"

internal val SqlDelightFile.queriesName
  get() = "${virtualFile!!.nameWithoutExtension.decapitalize()}$QUERIES_SUFFIX_NAME"
internal val SqlDelightFile.queriesType
  get() = ClassName(packageName!!, "${virtualFile!!.nameWithoutExtension.capitalize()}$QUERIES_SUFFIX_NAME")

internal val TRANSACTER_TYPE = ClassName("app.cash.sqldelight", "Transacter")
internal val TRANSACTER_IMPL_TYPE = ClassName("app.cash.sqldelight", "TransacterImpl")
internal val ASYNC_TRANSACTER_TYPE = ClassName("app.cash.sqldelight.async", "AsyncTransacter")
internal val ASYNC_TRANSACTER_IMPL_TYPE = ClassName("app.cash.sqldelight.async", "AsyncTransacterImpl")
