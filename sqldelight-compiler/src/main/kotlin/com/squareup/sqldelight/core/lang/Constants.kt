package com.squareup.sqldelight.core.lang

import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.intellij.openapi.vfs.VirtualFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler.allocateName

internal val CURSOR_TYPE = ClassName("com.squareup.sqldelight.db", "SqlCursor")
internal val CURSOR_NAME = "cursor"

internal val DRIVER_TYPE = ClassName("com.squareup.sqldelight.db", "SqlDriver")
internal val DRIVER_NAME = "driver"
internal val DATABASE_SCHEMA_TYPE = DRIVER_TYPE.nestedClass("Schema")

internal val CUSTOM_DATABASE_NAME = "database"

internal val IMPLEMENTATION_NAME = "Impl"

internal val ADAPTER_NAME = "Adapter"

internal val SqliteCreateTableStmt.adapterName
  get() = "${allocateName(tableName)}$ADAPTER_NAME"

internal val QUERY_TYPE = ClassName("com.squareup.sqldelight", "Query")

internal val QUERY_LIST_TYPE = ClassName("kotlin.collections", "MutableList")
    .parameterizedBy(QUERY_TYPE.parameterizedBy(STAR))

internal val MAPPER_NAME = "mapper"

internal val EXECUTE_METHOD = "execute"

val VirtualFile.queriesName
    get() = "${nameWithoutExtension.capitalize()}Queries"

internal val SqlDelightFile.queriesName
  get() = "${virtualFile!!.nameWithoutExtension.decapitalize()}Queries"
internal val SqlDelightFile.queriesType
  get() = ClassName(packageName, "${virtualFile!!.nameWithoutExtension.capitalize()}Queries")

internal val TRANSACTER_TYPE = ClassName("com.squareup.sqldelight", "Transacter")
internal val TRANSACTER_IMPL_TYPE = ClassName("com.squareup.sqldelight", "TransacterImpl")
