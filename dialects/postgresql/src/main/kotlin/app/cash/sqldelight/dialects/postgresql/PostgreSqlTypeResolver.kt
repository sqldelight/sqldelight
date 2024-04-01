package app.cash.sqldelight.dialects.postgresql

import app.cash.sqldelight.dialect.api.DialectType
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType.BLOB
import app.cash.sqldelight.dialect.api.PrimitiveType.BOOLEAN
import app.cash.sqldelight.dialect.api.PrimitiveType.INTEGER
import app.cash.sqldelight.dialect.api.PrimitiveType.REAL
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.QueryWithResults
import app.cash.sqldelight.dialect.api.ReturningQueryable
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialect.api.encapsulatingType
import app.cash.sqldelight.dialect.api.encapsulatingTypePreferringKotlin
import app.cash.sqldelight.dialects.postgresql.PostgreSqlType.BIG_INT
import app.cash.sqldelight.dialects.postgresql.PostgreSqlType.DATE
import app.cash.sqldelight.dialects.postgresql.PostgreSqlType.SMALL_INT
import app.cash.sqldelight.dialects.postgresql.PostgreSqlType.TIMESTAMP
import app.cash.sqldelight.dialects.postgresql.PostgreSqlType.TIMESTAMP_TIMEZONE
import app.cash.sqldelight.dialects.postgresql.grammar.mixins.AggregateExpressionMixin
import app.cash.sqldelight.dialects.postgresql.grammar.mixins.WindowFunctionMixin
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlDeleteStmtLimited
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlExtensionExpr
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlInsertStmt
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTypeName
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlUpdateStmtLimited
import com.alecstrong.sql.psi.core.psi.SqlBinaryAddExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryMultExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryPipeExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlLiteralExpr
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.psi.tree.TokenSet
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asTypeName

class PostgreSqlTypeResolver(private val parentResolver: TypeResolver) : TypeResolver by parentResolver {
  override fun definitionType(typeName: SqlTypeName): IntermediateType = with(typeName) {
    check(this is PostgreSqlTypeName)
    val type = IntermediateType(
      when {
        smallIntDataType != null -> PostgreSqlType.SMALL_INT
        intDataType != null -> PostgreSqlType.INTEGER
        bigIntDataType != null -> PostgreSqlType.BIG_INT
        numericDataType != null -> PostgreSqlType.NUMERIC
        approximateNumericDataType != null -> REAL
        stringDataType != null -> TEXT
        uuidDataType != null -> PostgreSqlType.UUID
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
        jsonDataType != null -> PostgreSqlType.JSON
        booleanDataType != null -> BOOLEAN
        blobDataType != null -> BLOB
        tsvectorDataType != null -> PostgreSqlType.TSVECTOR
        else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${this.text}")
      },
    )
    if (node.getChildren(null).map { it.text }.takeLast(2) == listOf("[", "]")) {
      return arrayIntermediateType(type)
    }
    return type
  }

  override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
    return functionExpr.postgreSqlFunctionType() ?: parentResolver.functionType(functionExpr)
  }

  private fun SqlFunctionExpr.postgreSqlFunctionType() = when (functionName.text.lowercase()) {
    "greatest" -> encapsulatingTypePreferringKotlin(
      exprList,
      SMALL_INT,
      PostgreSqlType.INTEGER,
      INTEGER,
      BIG_INT,
      REAL,
      TEXT,
      BLOB,
      TIMESTAMP_TIMEZONE,
      TIMESTAMP,
    )
    "least" -> encapsulatingTypePreferringKotlin(
      exprList,
      BLOB,
      TEXT,
      SMALL_INT,
      INTEGER,
      PostgreSqlType.INTEGER,
      BIG_INT,
      REAL,
      TIMESTAMP_TIMEZONE,
      TIMESTAMP,
    )
    "concat" -> encapsulatingType(exprList, TEXT)
    "substring", "replace" -> IntermediateType(TEXT).nullableIf(resolvedType(exprList[0]).javaType.isNullable)
    "starts_with" -> IntermediateType(BOOLEAN)
    "coalesce", "ifnull" -> {
      val exprType = exprList.first().postgreSqlType()
      if (isArrayType(exprType)) {
        exprType
      } else {
        encapsulatingTypePreferringKotlin(exprList, SMALL_INT, PostgreSqlType.INTEGER, INTEGER, BIG_INT, REAL, TEXT, BLOB, nullability = { exprListNullability ->
          exprListNullability.all { it }
        })
      }
    }
    "max" -> encapsulatingTypePreferringKotlin(exprList, SMALL_INT, PostgreSqlType.INTEGER, INTEGER, BIG_INT, REAL, TEXT, BLOB, TIMESTAMP_TIMEZONE, TIMESTAMP, DATE).asNullable()
    "min" -> encapsulatingTypePreferringKotlin(exprList, BLOB, TEXT, SMALL_INT, INTEGER, PostgreSqlType.INTEGER, BIG_INT, REAL, TIMESTAMP_TIMEZONE, TIMESTAMP, DATE).asNullable()
    "sum" -> {
      val type = resolvedType(exprList.single())
      if (type.dialectType == REAL) {
        IntermediateType(REAL).asNullable()
      } else {
        IntermediateType(INTEGER).asNullable()
      }
    }
    "to_hex", "quote_literal", "quote_ident", "md5" -> IntermediateType(TEXT)
    "quote_nullable" -> IntermediateType(TEXT).asNullable()
    "date_trunc" -> encapsulatingType(exprList, TIMESTAMP_TIMEZONE, TIMESTAMP)
    "date_part" -> IntermediateType(REAL)
    "percentile_disc" -> IntermediateType(REAL).asNullable()
    "now" -> IntermediateType(TIMESTAMP_TIMEZONE)
    "corr", "covar_pop", "covar_samp", "regr_avgx", "regr_avgy", "regr_intercept",
    "regr_r2", "regr_slope", "regr_sxx", "regr_sxy", "regr_syy",
    -> IntermediateType(REAL).asNullable()
    "stddev", "stddev_pop", "stddev_samp", "variance",
    "var_pop", "var_samp",
    -> if (resolvedType(exprList[0]).dialectType == REAL) {
      IntermediateType(REAL).asNullable()
    } else {
      IntermediateType(
        PostgreSqlType.NUMERIC,
      ).asNullable()
    }
    "regr_count" -> IntermediateType(BIG_INT).asNullable()
    "gen_random_uuid" -> IntermediateType(PostgreSqlType.UUID)
    "length", "character_length", "char_length" -> IntermediateType(PostgreSqlType.INTEGER).nullableIf(resolvedType(exprList[0]).javaType.isNullable)
    "to_json", "to_jsonb",
    "array_to_json", "row_to_json",
    "json_build_array", "jsonb_build_array",
    "json_object", "jsonb_object",
    "json_extract_path", "jsonb_extract_path",
    "json_extract_path_text", "jsonb_extract_path_text",
    "jsonb_set", "jsonb_set_lax", "jsonb_insert",
    "json_strip_nulls", "jsonb_strip_nulls",
    "jsonb_path_query_array", "jsonb_path_query_first", "jsonb_path_query_array_tz", "jsonb_path_query_first_tz",
    "jsonb_pretty",
    "json_typeof", "jsonb_typeof",
    "json_agg", "jsonb_agg", "json_object_agg", "jsonb_object_agg",
    -> IntermediateType(PostgreSqlType.JSON)
    "json_build_object", "jsonb_build_object",
    -> IntermediateType(TEXT)
    "array_agg" -> {
      val typeForAgg = encapsulatingTypePreferringKotlin(exprList, SMALL_INT, PostgreSqlType.INTEGER, INTEGER, BIG_INT, REAL, TEXT, TIMESTAMP_TIMEZONE, TIMESTAMP, DATE).asNullable()
      arrayIntermediateType(typeForAgg)
    }
    "string_agg" -> IntermediateType(TEXT)
    "json_array_length", "jsonb_array_length" -> IntermediateType(INTEGER)
    "jsonb_path_exists", "jsonb_path_match", "jsonb_path_exists_tz", "jsonb_path_match_tz" -> IntermediateType(BOOLEAN)
    "currval", "lastval", "nextval", "setval" -> IntermediateType(BIG_INT)
    "generate_series" -> encapsulatingType(exprList, INTEGER, BIG_INT, REAL, TIMESTAMP_TIMEZONE, TIMESTAMP)
    "regexp_count", "regexp_instr" -> IntermediateType(INTEGER)
    "regexp_like" -> IntermediateType(BOOLEAN)
    "regexp_replace", "regexp_substr" -> IntermediateType(TEXT)
    "to_tsquery" -> IntermediateType(TEXT)
    "to_tsvector" -> IntermediateType(PostgreSqlType.TSVECTOR)
    "ts_rank" -> encapsulatingType(exprList, REAL, TEXT)
    "websearch_to_tsquery" -> IntermediateType(TEXT)
    else -> null
  }

  override fun queryWithResults(sqlStmt: SqlStmt): QueryWithResults? {
    sqlStmt.insertStmt?.let { insert ->
      check(insert is PostgreSqlInsertStmt)
      insert.returningClause?.let { return ReturningQueryable(insert, it, insert.tableName) }
    }
    sqlStmt.updateStmtLimited?.let { update ->
      check(update is PostgreSqlUpdateStmtLimited)
      update.returningClause?.let { return ReturningQueryable(update, it, update.qualifiedTableName.tableName) }
    }
    sqlStmt.deleteStmtLimited?.let { delete ->
      check(delete is PostgreSqlDeleteStmtLimited)
      delete.returningClause?.let { return ReturningQueryable(delete, it, delete.qualifiedTableName?.tableName) }
    }
    return parentResolver.queryWithResults(sqlStmt)
  }

  override fun simplifyType(intermediateType: IntermediateType): IntermediateType {
    // Primary key columns are non null always.
    val columnDef = intermediateType.column ?: return intermediateType
    val tableDef = columnDef.parent as? SqlCreateTableStmt ?: return intermediateType
    tableDef.tableConstraintList.forEach {
      if (columnDef.columnName.name in it.indexedColumnList.mapNotNull {
          val expr = it.expr
          if (expr is SqlColumnExpr) expr.columnName.name else null
        }
      ) {
        return intermediateType.asNonNullable()
      }
    }

    return parentResolver.simplifyType(intermediateType)
  }

  override fun resolvedType(expr: SqlExpr): IntermediateType {
    return expr.postgreSqlType()
  }

  private fun SqlExpr.postgreSqlType(): IntermediateType = when (this) {
    is SqlBinaryExpr -> {
      if (node.findChildByType(binaryExprChildTypesResolvingToBool) != null) {
        IntermediateType(BOOLEAN)
      } else {
        encapsulatingType(
          exprList = getExprList(),
          nullability = { exprListNullability ->
            (this is SqlBinaryAddExpr || this is SqlBinaryMultExpr || this is SqlBinaryPipeExpr) &&
              exprListNullability.any { it }
          },
          SMALL_INT,
          PostgreSqlType.INTEGER,
          INTEGER,
          BIG_INT,
          REAL,
          TEXT,
          BLOB,
          PostgreSqlType.INTERVAL,
          PostgreSqlType.TIMESTAMP_TIMEZONE,
          PostgreSqlType.TIMESTAMP,
          PostgreSqlType.JSON,
          PostgreSqlType.TSVECTOR,
        )
      }
    }
    is SqlLiteralExpr -> when {
      literalValue.text == "CURRENT_DATE" -> IntermediateType(PostgreSqlType.DATE)
      literalValue.text == "CURRENT_TIME" -> IntermediateType(PostgreSqlType.TIME)
      literalValue.text == "CURRENT_TIMESTAMP" -> IntermediateType(PostgreSqlType.TIMESTAMP)
      literalValue.text.startsWith("INTERVAL") -> IntermediateType(PostgreSqlType.INTERVAL)
      else -> parentResolver.resolvedType(this)
    }
    is PostgreSqlExtensionExpr -> when {
      arrayAggStmt != null -> {
        val typeForArray = (arrayAggStmt as AggregateExpressionMixin).expr.postgreSqlType() // same as resolvedType(expr)
        arrayIntermediateType(typeForArray)
      }
      stringAggStmt != null -> {
        IntermediateType(TEXT)
      }
      windowFunctionExpr != null -> {
        val windowFunctionExpr = windowFunctionExpr as WindowFunctionMixin
        functionType(windowFunctionExpr.functionExpr)!!
      }
      jsonExpression != null -> {
        if (jsonExpression!!.jsonbBooleanOperator != null) {
          IntermediateType(BOOLEAN)
        } else {
          IntermediateType(PostgreSqlType.JSON)
        }
      }
      matchOperatorExpression != null -> {
        IntermediateType(BOOLEAN)
      }
      else -> parentResolver.resolvedType(this)
    }

    else -> parentResolver.resolvedType(this)
  }

  companion object {
    private val binaryExprChildTypesResolvingToBool = TokenSet.create(
      SqlTypes.EQ,
      SqlTypes.EQ2,
      SqlTypes.NEQ,
      SqlTypes.NEQ2,
      SqlTypes.AND,
      SqlTypes.OR,
      SqlTypes.GT,
      SqlTypes.GTE,
      SqlTypes.LT,
      SqlTypes.LTE,
    )

    private fun arrayIntermediateType(type: IntermediateType): IntermediateType {
      return IntermediateType(
        object : DialectType {
          override val javaType = Array::class.asTypeName().parameterizedBy(type.javaType)
          override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock) =
            CodeBlock.of("bindObject(%L, %L)\n", columnIndex, value)
          override fun cursorGetter(columnIndex: Int, cursorName: String) =
            CodeBlock.of("$cursorName.getArray<%T>($columnIndex)", type.javaType)
        },
      )
    }

    private fun isArrayType(type: IntermediateType): Boolean {
      return type.javaType.toString().startsWith("kotlin.Array")
    }
  }
}
