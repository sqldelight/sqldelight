package com.squareup.sqldelight.core.lang

import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.intellij.openapi.vfs.VirtualFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler.allocateName

internal val CURSOR_TYPE = ClassName("com.squareup.sqldelight.db", "SqlCursor")
internal val CURSOR_NAME = "cursor"

internal val DATABASE_TYPE = ClassName("com.squareup.sqldelight.db", "SqlDatabase")
internal val DATABASE_NAME = "database"
internal val DATABASE_SCHEMA_TYPE = DATABASE_TYPE.nestedClass("Schema")

internal val QUERY_WRAPPER_NAME = "queryWrapper"

internal val IMPLEMENTATION_NAME = "Impl"

internal val ADAPTER_NAME = "Adapter"

internal val SqliteCreateTableStmt.adapterName
  get() = "${allocateName(tableName)}$ADAPTER_NAME"

internal val STATEMENT_NAME = "statement"
internal val STATEMENT_TYPE = ClassName("com.squareup.sqldelight.db", "SqlPreparedStatement")
internal val STATEMENT_TYPE_ENUM = STATEMENT_TYPE.nestedClass("Type")

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
internal val TRANSACTION_TYPE = TRANSACTER_TYPE.nestedClass("Transaction")

internal fun isUnchangedPropertyName(name: String) =
  name.startsWith("is") && !name[2].isLowerCase()
