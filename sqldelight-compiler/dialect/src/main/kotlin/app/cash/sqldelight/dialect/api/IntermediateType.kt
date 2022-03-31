package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.squareup.kotlinpoet.TypeName

/**
 * Internal representation for a column type, which has dialect data affinity as well as JVM class
 * type.
 */
data class IntermediateType(
  val dialectType: DialectType,
  val javaType: TypeName = dialectType.javaType,
  /**
   * The column definition this type is sourced from, or null if there is none.
   */
  val column: SqlColumnDef? = null,
  /**
   * The name of this intermediate type as exposed in the generated api.
   */
  val name: String = "value",
  /**
   * The original bind argument expression this intermediate type comes from.
   */
  val bindArg: SqlBindExpr? = null,
  /**
   * The types assumed to be compatible with this type. Validated at runtime.
   */
  val assumedCompatibleTypes: List<IntermediateType> = emptyList(),
  /**
   * If this type has been simplified by the [SqlDelightDialect], it may not need things like
   * an adapter.
   */
  val simplified: Boolean = false,
) {
  fun asNullable() = copy(javaType = javaType.copy(nullable = true))

  fun asNonNullable() = copy(javaType = javaType.copy(nullable = false))

  fun nullableIf(predicate: Boolean): IntermediateType {
    return if (predicate) asNullable() else asNonNullable()
  }
}
