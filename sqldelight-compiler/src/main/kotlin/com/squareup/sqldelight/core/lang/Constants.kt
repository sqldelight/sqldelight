package com.squareup.sqldelight.core.lang

import com.intellij.openapi.vfs.VirtualFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR

internal val CURSOR_TYPE = ClassName("com.squareup.sqldelight.db", "SqlCursor")
internal const val CURSOR_NAME = "cursor"

internal val DRIVER_TYPE = ClassName("com.squareup.sqldelight.db", "SqlDriver")
internal const val DRIVER_NAME = "driver"
internal val DATABASE_SCHEMA_TYPE = DRIVER_TYPE.nestedClass("Schema")

internal const val CUSTOM_DATABASE_NAME = "database"

internal const val ADAPTER_NAME = "Adapter"

internal val QUERY_TYPE = ClassName("com.squareup.sqldelight", "Query")

internal val QUERY_LIST_TYPE = ClassName("kotlin.collections", "MutableList")
  .parameterizedBy(QUERY_TYPE.parameterizedBy(STAR))

internal const val MAPPER_NAME = "mapper"

internal const val EXECUTE_METHOD = "execute"

const val QUERIES_SUFFIX_NAME = "Queries"

val VirtualFile.queriesName
  get() = "${nameWithoutExtension.capitalize()}$QUERIES_SUFFIX_NAME"

internal val SqlDelightFile.queriesName
  get() = "${virtualFile!!.nameWithoutExtension.decapitalize()}$QUERIES_SUFFIX_NAME"
internal val SqlDelightFile.queriesType
  get() = ClassName(packageName!!, "${virtualFile!!.nameWithoutExtension.capitalize()}$QUERIES_SUFFIX_NAME")
internal fun SqlDelightFile.queriesImplType(implementationPackage: String) =
  ClassName(implementationPackage, "${virtualFile!!.nameWithoutExtension.capitalize()}QueriesImpl")

internal val TRANSACTER_TYPE = ClassName("com.squareup.sqldelight", "Transacter")
internal val TRANSACTER_IMPL_TYPE = ClassName("com.squareup.sqldelight", "TransacterImpl")
