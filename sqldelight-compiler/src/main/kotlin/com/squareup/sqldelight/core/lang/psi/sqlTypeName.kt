package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sql.psi.core.mysql.psi.MySqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.alecstrong.sql.psi.core.sqlite_3_18.psi.TypeName as SqliteTypeName
import com.squareup.sqldelight.core.lang.IntermediateType

internal fun SqlTypeName.type(): IntermediateType {
  return when (this) {
    is SqliteTypeName -> type()
    is MySqlTypeName -> type()
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

private fun MySqlTypeName.type(): IntermediateType {
  return when {
    approximateNumericDataType != null -> IntermediateType(IntermediateType.SqliteType.REAL)
    binaryDataType != null -> IntermediateType(IntermediateType.SqliteType.BLOB)
    dateDataType != null -> IntermediateType(IntermediateType.SqliteType.TEXT)
    exactNumericDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER)
    jsonDataType != null -> IntermediateType(IntermediateType.SqliteType.TEXT)
    stringDataType != null -> IntermediateType(IntermediateType.SqliteType.TEXT)
    else -> throw AssertionError("Unknown kotlin type for sql type $this")
  }
}
