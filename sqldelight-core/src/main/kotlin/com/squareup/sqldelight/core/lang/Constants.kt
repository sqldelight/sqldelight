package com.squareup.sqldelight.core.lang

import com.alecstrong.sqlite.psi.core.psi.SqliteCreateTableStmt
import com.squareup.kotlinpoet.ClassName

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