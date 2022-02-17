package app.cash.sqldelight.core.lang

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

private val QUERY_TYPE = ClassName("app.cash.sqldelight", "Query")

/**
 * Parameterize a `Query` by two types: [dataType] and [cursorType].
 */
fun parameterizeQueryBy(dataType: TypeName, cursorType: TypeName) = QUERY_TYPE.parameterizedBy(
  dataType, cursorType
)

const val QUERIES_SUFFIX_NAME = "Queries"
internal val QUERY_FUNCTION = ClassName("app.cash.sqldelight", "Query")
internal val QUERY_LISTENER_TYPE = QUERY_TYPE.nestedClass("Listener")
internal val QUERY_LISTENER_LIST_TYPE = ClassName("kotlin.collections", "MutableList")
  .parameterizedBy(QUERY_LISTENER_TYPE)
