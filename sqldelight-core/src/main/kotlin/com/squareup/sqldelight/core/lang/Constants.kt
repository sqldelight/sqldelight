package com.squareup.sqldelight.core.lang

import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName

internal val RESULT_SET_TYPE = ClassName("com.squareup.sqldelight.db", "SqlResultSet")
internal val RESULT_SET_NAME = "resultSet"

internal val DATABASE_TYPE = ClassName("com.squareup.sqldelight.db", "SqlDatabase")
internal val DATABASE_NAME = "database"

internal val QUERY_WRAPPER_NAME = "queryWrapper"

internal val ADAPTER_NAME = "Adapter"

internal val SqliteCreateTableStmt.adapterName
  get() = "${tableName.name}$ADAPTER_NAME"

internal val STATEMENT_NAME = "statement"
internal val STATEMENT_TYPE = ClassName("com.squareup.sqldelight.db", "SqlPreparedStatement")

internal val QUERY_TYPE = ClassName("com.squareup.sqldelight", "Query")

internal val DIRTIED_FUNCTION = "dirtied"

internal val MAPPER_NAME = "mapper"

internal val EXECUTE_METHOD = "execute"
internal val EXECUTE_RESULT = "result"

internal val SqlDelightFile.queriesName
  get() = "${virtualFile.nameWithoutExtension.decapitalize()}Queries"
internal val SqlDelightFile.queriesType
  get() = ClassName(packageName, "${virtualFile.nameWithoutExtension}Queries")

internal val TRANSACTER_TYPE = ClassName("com.squareup.sqldelight", "Transacter")
internal val TRANSACTION_TYPE = TRANSACTER_TYPE.nestedClass("Transaction")
internal val TRANSACTIONS_NAME = "transactions"

// TODO: Oh fuck threadlocal is a java type. Ill think about this later
internal val THREADLOCAL_TYPE = ThreadLocal::class.asClassName()

internal val CONNECTION_TYPE = ClassName("com.squareup.sqldelight.db", "SqlDatabaseConnection")
internal val CONNECTION_NAME = "db"
