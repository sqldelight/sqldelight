package app.cash.sqldelight.dialects.postgresql

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.PrimitiveType.BLOB
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.QueryWithResults
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.encapsulatingType
import app.cash.sqldelight.dialects.postgresql.PostgreSqlType.BIG_INT
import app.cash.sqldelight.dialects.postgresql.PostgreSqlType.SMALL_INT
import app.cash.sqldelight.dialects.postgresql.PostgreSqlType.TIMESTAMP
import app.cash.sqldelight.dialects.postgresql.PostgreSqlType.TIMESTAMP_TIMEZONE
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlInsertStmt
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlTypeName

class PostgreSqlTypeResolver(private val parentResolver: TypeResolver) : TypeResolver by parentResolver {
  override fun definitionType(typeName: SqlTypeName): IntermediateType = with(typeName) {
    check(this is PostgreSqlTypeName)
    return IntermediateType(
      when {
        smallIntDataType != null -> PostgreSqlType.SMALL_INT
        intDataType != null -> PostgreSqlType.INTEGER
        bigIntDataType != null -> PostgreSqlType.BIG_INT
        numericDataType != null -> REAL
        approximateNumericDataType != null -> REAL
        stringDataType != null -> TEXT
        smallSerialDataType != null -> PostgreSqlType.SMALL_INT
        serialDataType != null -> PostgreSqlType.INTEGER
        bigSerialDataType != null -> PostgreSqlType.BIG_INT
        dateDataType != null -> {
          when (dateDataType!!.firstChild.text) {
            "DATE" -> PostgreSqlType.DATE
            "TIME" -> PostgreSqlType.TIME
            "TIMESTAMP" -> if (dateDataType!!.node.getChildren(null).any { it.text == "WITH" }) TIMESTAMP_TIMEZONE else TIMESTAMP
            "TIMESTAMPTZ" -> TIMESTAMP_TIMEZONE
            "INTERVAL" -> PostgreSqlType.INTERVAL
            else -> throw IllegalArgumentException("Unknown date type ${dateDataType!!.text}")
          }
        }
        jsonDataType != null -> TEXT
        booleanDataType != null -> PrimitiveType.BOOLEAN
        blobDataType != null -> BLOB
        else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${this.text}")
      }
    )
  }

  override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
    return functionExpr.postgreSqlFunctionType() ?: parentResolver.functionType(functionExpr)
  }

  private fun SqlFunctionExpr.postgreSqlFunctionType() = when (functionName.text.toLowerCase()) {
    "greatest" -> encapsulatingType(exprList, PrimitiveType.INTEGER, REAL, TEXT, BLOB)
    "concat" -> encapsulatingType(exprList, TEXT)
    "substring" -> IntermediateType(TEXT).nullableIf(resolvedType(exprList[0]).javaType.isNullable)
    "coalesce", "ifnull" -> encapsulatingType(exprList, SMALL_INT, PostgreSqlType.INTEGER, INTEGER, BIG_INT, REAL, TEXT, BLOB)
    "max" -> encapsulatingType(exprList, SMALL_INT, PostgreSqlType.INTEGER, INTEGER, BIG_INT, REAL, TEXT, BLOB).asNullable()
    "min" -> encapsulatingType(exprList, BLOB, TEXT, SMALL_INT, INTEGER, PostgreSqlType.INTEGER, BIG_INT, REAL).asNullable()
    else -> null
  }

  override fun queryWithResults(sqlStmt: SqlStmt): QueryWithResults? {
    sqlStmt.insertStmt?.let { insert ->
      check(insert is PostgreSqlInsertStmt)
      insert.returningClause?.let {
        return object : QueryWithResults {
          override var statement: SqlAnnotatedElement = insert
          override val select = it
          override val pureTable = insert.tableName
        }
      }
    }
    return parentResolver.queryWithResults(sqlStmt)
  }
}
