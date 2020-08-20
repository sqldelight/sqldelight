package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sql.psi.core.hsql.psi.HsqlTypeName
import com.alecstrong.sql.psi.core.mysql.psi.MySqlTypeName
import com.alecstrong.sql.psi.core.postgresql.psi.PostgreSqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.alecstrong.sql.psi.core.sqlite_3_18.psi.TypeName as SqliteTypeName
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.sqldelight.core.lang.IntermediateType

internal fun SqlTypeName.type(): IntermediateType {
  return when (this) {
    is SqliteTypeName -> type()
    is MySqlTypeName -> type()
    is PostgreSqlTypeName -> type()
    is HsqlTypeName -> type()
    else -> throw IllegalArgumentException("Unknown sql type $this")
  }
}

private fun SqliteTypeName.type(): IntermediateType {
  return when (text) {
    "TEXT" -> IntermediateType(IntermediateType.SqliteType.TEXT)
    "BLOB" -> IntermediateType(IntermediateType.SqliteType.BLOB)
    "INTEGER" -> IntermediateType(IntermediateType.SqliteType.INTEGER)
    "REAL" -> IntermediateType(IntermediateType.SqliteType.REAL)
    else -> throw IllegalArgumentException("Unknown sql type $text")
  }
}

private fun MySqlTypeName.type(): IntermediateType {
  return when {
    approximateNumericDataType != null -> IntermediateType(IntermediateType.SqliteType.REAL)
    binaryDataType != null -> IntermediateType(IntermediateType.SqliteType.BLOB)
    dateDataType != null -> IntermediateType(IntermediateType.SqliteType.TEXT)
    tinyIntDataType != null -> if (tinyIntDataType!!.text == "BOOLEAN") {
      IntermediateType(IntermediateType.SqliteType.INTEGER, BOOLEAN)
    } else {
      IntermediateType(IntermediateType.SqliteType.INTEGER, BYTE)
    }
    smallIntDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER, SHORT)
    mediumIntDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER, INT)
    intDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER, INT)
    bigIntDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER, LONG)
    fixedPointDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER)
    jsonDataType != null -> IntermediateType(IntermediateType.SqliteType.TEXT)
    enumSetType != null -> IntermediateType(IntermediateType.SqliteType.TEXT)
    characterType != null -> IntermediateType(IntermediateType.SqliteType.TEXT)
    bitDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER, BOOLEAN)
    else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${this.text}")
  }
}

private fun PostgreSqlTypeName.type(): IntermediateType {
  return when {
    smallIntDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER, SHORT)
    intDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER, INT)
    bigIntDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER, LONG)
    numericDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER)
    approximateNumericDataType != null -> IntermediateType(IntermediateType.SqliteType.REAL)
    stringDataType != null -> IntermediateType(IntermediateType.SqliteType.TEXT)
    dateDataType != null -> IntermediateType(IntermediateType.SqliteType.TEXT)
    jsonDataType != null -> IntermediateType(IntermediateType.SqliteType.TEXT)
    else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${this.text}")
  }
}

private fun HsqlTypeName.type(): IntermediateType {
  return when {
    approximateNumericDataType != null -> IntermediateType(IntermediateType.SqliteType.REAL)
    binaryStringDataType != null -> IntermediateType(IntermediateType.SqliteType.BLOB)
    dateDataType != null -> IntermediateType(IntermediateType.SqliteType.TEXT)
    tinyIntDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER, BYTE)
    smallIntDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER, SHORT)
    intDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER, INT)
    bigIntDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER, LONG)
    fixedPointDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER)
    characterStringDataType != null -> IntermediateType(IntermediateType.SqliteType.TEXT)
    booleanDataType != null -> IntermediateType(IntermediateType.SqliteType.INTEGER, BOOLEAN)
    bitStringDataType != null -> IntermediateType(IntermediateType.SqliteType.BLOB)
    intervalDataType != null -> IntermediateType(IntermediateType.SqliteType.BLOB)
    else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${this.text}")
  }
}
