package app.cash.sqldelight.core.lang

import app.cash.sqldelight.core.capitalize
import app.cash.sqldelight.core.decapitalize
import com.intellij.openapi.vfs.VirtualFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal val CURSOR_TYPE = ClassName("app.cash.sqldelight.db", "SqlCursor")
internal const val CURSOR_NAME = "cursor"

internal val DRIVER_TYPE = ClassName("app.cash.sqldelight.db", "SqlDriver")
internal const val DRIVER_NAME = "driver"
internal val DATABASE_SCHEMA_TYPE = ClassName("app.cash.sqldelight.db", "SqlSchema")

internal val QUERY_RESULT_TYPE = ClassName("app.cash.sqldelight.db", "QueryResult")
internal val VALUE_RESULT_TYPE = QUERY_RESULT_TYPE.nestedClass("Value")
internal val ASYNC_RESULT_TYPE = QUERY_RESULT_TYPE.nestedClass("AsyncValue")
internal val UNIT_RESULT_TYPE = QUERY_RESULT_TYPE.nestedClass("Unit")

internal val AFTER_VERSION_TYPE = ClassName("app.cash.sqldelight.db", "AfterVersion")

internal val PREPARED_STATEMENT_TYPE = ClassName("app.cash.sqldelight.db", "SqlPreparedStatement")

internal const val CUSTOM_DATABASE_NAME = "database"

internal const val ADAPTER_NAME = "Adapter"

internal val QUERY_TYPE = ClassName("app.cash.sqldelight", "Query")
internal val EXECUTABLE_QUERY_TYPE = ClassName("app.cash.sqldelight", "ExecutableQuery")
internal val QUERY_LISTENER_TYPE = QUERY_TYPE.nestedClass("Listener")
internal val QUERY_LISTENER_LIST_TYPE = ClassName("kotlin.collections", "MutableList")
  .parameterizedBy(QUERY_LISTENER_TYPE)

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
internal val SUSPENDING_TRANSACTER_TYPE = ClassName("app.cash.sqldelight", "SuspendingTransacter")
internal val SUSPENDING_TRANSACTER_IMPL_TYPE = ClassName("app.cash.sqldelight", "SuspendingTransacterImpl")
