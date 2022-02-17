package app.cash.sqldelight.core.lang

import com.intellij.openapi.vfs.VirtualFile
import com.squareup.kotlinpoet.ClassName

internal const val CUSTOM_DATABASE_NAME = "database"

internal const val ADAPTER_NAME = "Adapter"

internal const val MAPPER_NAME = "mapper"

internal const val EXECUTE_METHOD = "execute"

internal val CURSOR_TYPE = ClassName("app.cash.sqldelight.db", "SqlCursor")
internal const val CURSOR_NAME = "cursor"

internal val PREPARED_STATEMENT_TYPE = ClassName("app.cash.sqldelight.db", "SqlPreparedStatement")

val VirtualFile.queriesName
  get() = "${nameWithoutExtension.capitalize()}$QUERIES_SUFFIX_NAME"

internal val SqlDelightFile.queriesName
  get() = "${virtualFile!!.nameWithoutExtension.decapitalize()}$QUERIES_SUFFIX_NAME"
internal val SqlDelightFile.queriesType
  get() = ClassName(packageName!!, "${virtualFile!!.nameWithoutExtension.capitalize()}$QUERIES_SUFFIX_NAME")

internal val TRANSACTER_TYPE = ClassName("app.cash.sqldelight", "Transacter")
internal val TRANSACTER_IMPL_TYPE = ClassName("app.cash.sqldelight", "TransacterImpl")
