package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlTypeName

interface TypeResolver {
  /**
   * @param expr The expression to be resolved to a type.
   * @return The type for [expr] for null if this resolver cannot solve.
   */
  fun resolvedType(expr: SqlExpr): IntermediateType

  fun argumentType(bindArg: SqlBindExpr): IntermediateType

  /**
   * In the context of [parent], @return the type [argument] (which is a child expression) should have.
   */
  fun argumentType(parent: SqlExpr, argument: SqlExpr): IntermediateType

  fun functionType(functionExpr: SqlFunctionExpr): IntermediateType?

  /**
   * @return the dialect specific (or [PrimitiveType]) for a type name.
   */
  fun definitionType(typeName: SqlTypeName): IntermediateType
}

/**
 * @return the type from the expr list which is the highest order in the typeOrder list
 */
fun TypeResolver.encapsulatingType(
  exprList: List<SqlExpr>,
  vararg typeOrder: DialectType
) = encapsulatingType(exprList = exprList, nullableIfAny = false, typeOrder = typeOrder)

/**
 * @return the type from the expr list which is the highest order in the typeOrder list
 */
fun TypeResolver.encapsulatingType(
  exprList: List<SqlExpr>,
  nullableIfAny: Boolean,
  vararg typeOrder: DialectType
): IntermediateType {
  val types = exprList.map { resolvedType(it) }
  val sqlTypes = types.map { it.dialectType }

  val type = typeOrder.last { it in sqlTypes }
  if (!nullableIfAny && types.all { it.javaType.isNullable } ||
    nullableIfAny && types.any { it.javaType.isNullable }
  ) {
    return IntermediateType(type).asNullable()
  }
  return IntermediateType(type)
}
