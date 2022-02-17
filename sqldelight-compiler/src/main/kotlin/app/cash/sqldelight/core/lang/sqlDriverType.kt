package app.cash.sqldelight.core.lang

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

private val DRIVER_TYPE = ClassName("app.cash.sqldelight.db", "SqlDriver")
private val DATABASE_SCHEMA_TYPE = DRIVER_TYPE.nestedClass("Schema")

/**
 * Parameterize a `SqlDriver` by two type parameters: [statementType] and [cursorType].
 */
fun parameterizeSqlDriverBy(statementType: TypeName, cursorType: TypeName) = DRIVER_TYPE.parameterizedBy(
  statementType, cursorType
)

/**
 * Parameterize a `SqlDriver.Schema` by two type parameters: [statementType] and [cursorType].
 */
fun parameterizeSchemaBy(statementType: TypeName, cursorType: TypeName) = DATABASE_SCHEMA_TYPE.parameterizedBy(
  statementType, cursorType
)

internal const val DRIVER_NAME = "driver"
