package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlTypeName
import com.intellij.psi.PsiElement

interface TypeResolver {
  /**
   * @param expr The expression to be resolved to a type.
   * @return The resolved type
   */
  fun resolvedType(expr: SqlExpr): IntermediateType

  /**
   * In the context of [parent], @return the type [argument] (which is a child expression) should have.
   */
  fun argumentType(parent: PsiElement, argument: SqlExpr): IntermediateType

  /**
   * Resolves the type of dialect specific functions
   */
  fun functionType(functionExpr: SqlFunctionExpr): IntermediateType?

  /**
   * @return the dialect specific (or [PrimitiveType]) for a type name.
   */
  fun definitionType(typeName: SqlTypeName): IntermediateType

  /**
   * Enables a dialect to simplify a pure SQLDelight Intermediate Type into something the
   * dialect can deal with natively. (ie Integer AS Boolean in SQLite can be handled without an
   * adapter).
   */
  fun simplifyType(intermediateType: IntermediateType): IntermediateType = intermediateType

  fun queryWithResults(sqlStmt: SqlStmt): QueryWithResults?
}

/**
 * @return the type from the expr list which is the highest order in the typeOrder list
 */
fun TypeResolver.encapsulatingType(
  exprList: List<SqlExpr>,
  vararg typeOrder: DialectType,
) = encapsulatingType(exprList = exprList, nullability = null, typeOrder = typeOrder)

/**
 * @return the type from the expr list which is the highest order in the typeOrder list
 */
fun TypeResolver.encapsulatingTypePreferringKotlin(
  exprList: List<SqlExpr>,
  vararg typeOrder: DialectType,
  nullability: ((List<Boolean>) -> Boolean)? = null,
) = encapsulatingType(exprList = exprList, nullability = nullability, preferKotlinType = true, typeOrder = typeOrder)

/**
 * @return the type from the expr list which is the highest order in the typeOrder list
 */
fun TypeResolver.encapsulatingType(
  exprList: List<SqlExpr>,
  nullability: ((List<Boolean>) -> Boolean)?,
  vararg typeOrder: DialectType,
  preferKotlinType: Boolean = false,
): IntermediateType {
  val types = exprList.map { resolvedType(it) }
  val sqlTypes = types.map { it.dialectType }

  if (PrimitiveType.ARGUMENT in sqlTypes) {
    if (typeOrder.size == 1) {
      return IntermediateType(typeOrder.single())
    }
    val otherFunctionParameters = sqlTypes.distinct() - PrimitiveType.ARGUMENT
    if (otherFunctionParameters.size == 1) {
      return IntermediateType(otherFunctionParameters.single())
    }
    error("The Kotlin type of the argument cannot be inferred, use CAST instead.")
  }

  // stripping nullability because that shouldn't affect the type comparison
  val isTypesHomogeneous = types.map { it.dialectType to it.javaType.copy(nullable = false) }.distinct().size == 1
  val type = if (preferKotlinType && isTypesHomogeneous) {
    types.first()
  } else {
    IntermediateType(typeOrder.last { it in sqlTypes })
  }

  val exprListNullability = types.map { it.javaType.isNullable }
  return when (nullability) {
    null -> when {
      exprListNullability.all { it } -> type.asNullable()
      else -> type
    }
    else -> type.nullableIf(nullability(exprListNullability))
  }
}
