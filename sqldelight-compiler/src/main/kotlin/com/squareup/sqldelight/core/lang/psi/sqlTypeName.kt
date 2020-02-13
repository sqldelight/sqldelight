package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.squareup.sqldelight.core.lang.IntermediateType
import com.alecstrong.sql.psi.core.sqlite_3_18.psi.TypeName as SqliteTypeName

internal fun SqlTypeName.type(): IntermediateType {
  return when (this) {
    is SqliteTypeName -> type()
    else -> throw AssertionError("Unknown sql type $this")
  }
}

private fun SqliteTypeName.type(): IntermediateType {
  return when (text) {
    "TEXT" -> IntermediateType(IntermediateType.SqliteType.TEXT)
    "BLOB" -> IntermediateType(IntermediateType.SqliteType.BLOB)
    "INTEGER" -> IntermediateType(IntermediateType.SqliteType.INTEGER)
    "REAL" -> IntermediateType(IntermediateType.SqliteType.REAL)
    else -> throw AssertionError()
  }
}