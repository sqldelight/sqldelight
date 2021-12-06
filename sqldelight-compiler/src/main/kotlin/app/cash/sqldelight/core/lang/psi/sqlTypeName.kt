package app.cash.sqldelight.core.lang.psi

import app.cash.sqldelight.core.dialect.hsql.HsqlType
import app.cash.sqldelight.core.dialect.mysql.MySqlType
import app.cash.sqldelight.core.dialect.postgresql.PostgreSqlType
import app.cash.sqldelight.core.dialect.sqlite.SqliteType
import app.cash.sqldelight.core.lang.IntermediateType
import com.alecstrong.sql.psi.core.hsql.psi.HsqlTypeName
import com.alecstrong.sql.psi.core.mysql.psi.MySqlTypeName
import com.alecstrong.sql.psi.core.postgresql.psi.PostgreSqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.alecstrong.sql.psi.core.sqlite_3_18.psi.SqliteTypeName

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
  return when {
    textDataType != null -> IntermediateType(SqliteType.TEXT)
    blobDataType != null -> IntermediateType(SqliteType.BLOB)
    intDataType != null -> IntermediateType(SqliteType.INTEGER)
    realDataType != null -> IntermediateType(SqliteType.REAL)
    else -> throw IllegalArgumentException("Unknown sql type $text")
  }
}

private fun MySqlTypeName.type(): IntermediateType {
  return when {
    approximateNumericDataType != null -> IntermediateType(SqliteType.REAL)
    binaryDataType != null -> IntermediateType(SqliteType.BLOB)
    dateDataType != null -> IntermediateType(SqliteType.TEXT)
    tinyIntDataType != null -> if (tinyIntDataType!!.text == "BOOLEAN") {
      IntermediateType(MySqlType.TINY_INT_BOOL)
    } else {
      IntermediateType(MySqlType.TINY_INT)
    }
    smallIntDataType != null -> IntermediateType(MySqlType.SMALL_INT)
    mediumIntDataType != null -> IntermediateType(MySqlType.INTEGER)
    intDataType != null -> IntermediateType(MySqlType.INTEGER)
    bigIntDataType != null -> IntermediateType(MySqlType.BIG_INT)
    fixedPointDataType != null -> IntermediateType(SqliteType.INTEGER)
    jsonDataType != null -> IntermediateType(SqliteType.TEXT)
    enumSetType != null -> IntermediateType(SqliteType.TEXT)
    characterType != null -> IntermediateType(SqliteType.TEXT)
    bitDataType != null -> IntermediateType(MySqlType.BIT)
    else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${this.text}")
  }
}

private fun PostgreSqlTypeName.type(): IntermediateType {
  return when {
    smallIntDataType != null -> IntermediateType(PostgreSqlType.SMALL_INT)
    intDataType != null -> IntermediateType(PostgreSqlType.INTEGER)
    bigIntDataType != null -> IntermediateType(PostgreSqlType.BIG_INT)
    numericDataType != null -> IntermediateType(SqliteType.REAL)
    approximateNumericDataType != null -> IntermediateType(SqliteType.REAL)
    stringDataType != null -> IntermediateType(SqliteType.TEXT)
    smallSerialDataType != null -> IntermediateType(PostgreSqlType.SMALL_INT)
    serialDataType != null -> IntermediateType(PostgreSqlType.INTEGER)
    bigSerialDataType != null -> IntermediateType(PostgreSqlType.BIG_INT)
    dateDataType != null -> IntermediateType(SqliteType.TEXT)
    jsonDataType != null -> IntermediateType(SqliteType.TEXT)
    else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${this.text}")
  }
}

private fun HsqlTypeName.type(): IntermediateType {
  return when {
    approximateNumericDataType != null -> IntermediateType(SqliteType.REAL)
    binaryStringDataType != null -> IntermediateType(SqliteType.BLOB)
    dateDataType != null -> IntermediateType(SqliteType.TEXT)
    tinyIntDataType != null -> IntermediateType(HsqlType.TINY_INT)
    smallIntDataType != null -> IntermediateType(HsqlType.SMALL_INT)
    intDataType != null -> IntermediateType(HsqlType.INTEGER)
    bigIntDataType != null -> IntermediateType(HsqlType.BIG_INT)
    fixedPointDataType != null -> IntermediateType(SqliteType.INTEGER)
    characterStringDataType != null -> IntermediateType(SqliteType.TEXT)
    booleanDataType != null -> IntermediateType(HsqlType.BOOL)
    bitStringDataType != null -> IntermediateType(SqliteType.BLOB)
    intervalDataType != null -> IntermediateType(SqliteType.BLOB)
    else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${this.text}")
  }
}
