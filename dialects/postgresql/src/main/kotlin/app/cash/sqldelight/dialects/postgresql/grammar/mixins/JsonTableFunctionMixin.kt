package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType.TEXT
import app.cash.sqldelight.dialect.api.TableFunctionExprRowType
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.postgresql.PostgreSqlType
import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableFunctionAliasName
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableFunctionColumnAlias
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTableOrSubquery
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlNamedElementImpl
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.util.PsiTreeUtil

/**
 * A JSON set-returning function used in a FROM clause, e.g.
 * `SELECT json_object_keys FROM json_object_keys('{"a":1}')` or
 * `SELECT t.key, t.value FROM json_each('{"a":1}') AS t(key, value)`.
 *
 * Output column types are fixed by the function (see [jsonTableFunctionRowType]); every column
 * (`text`, `json`, `jsonb`) maps to Kotlin `String`. Postgres names the default columns `value`
 * (array_elements) or `key`/`value` (each), which have no element to resolve against, so those
 * functions require explicit column aliases. `*_object_keys`, whose default column name *is* the
 * function name, may be used without an alias.
 */
internal abstract class JsonTableFunctionMixin(
  node: ASTNode,
) : SqlNamedElementImpl(node),
  TableFunctionExprRowType {

  // Used when the function node is itself the single exposed column (un-aliased `*_object_keys`).
  override fun rowType(typeResolver: TypeResolver): IntermediateType = jsonTableFunctionRowType(name.lowercase(), 0)

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val functionName = name.lowercase()
    val tableOrSubquery = PsiTreeUtil.getParentOfType(this, PostgreSqlTableOrSubquery::class.java) ?: return
    val aliasColumnCount = tableOrSubquery.children.filterIsInstance<PostgreSqlTableFunctionAliasName>()
      .flatMap { it.children.filterIsInstance<PostgreSqlTableFunctionColumnAlias>() }
      .count()
    val expected = jsonTableFunctionColumnCount(functionName)
    if (jsonTableFunctionAllowsNoAlias(functionName)) {
      if (aliasColumnCount > expected) {
        annotationHolder.createErrorAnnotation(this, "$name exposes $expected column, found $aliasColumnCount column aliases")
      }
    } else if (aliasColumnCount != expected) {
      val example = if (expected == 1) "$name(...) AS t(value)" else "$name(...) AS t(key, value)"
      annotationHolder.createErrorAnnotation(this, "$name requires $expected column aliases, e.g. $example")
    }
  }

  override val parseRule: (PsiBuilder, Int) -> Boolean = PostgreSqlParser::json_table_function_real
}

/** Number of output columns of a JSON set-returning function. */
internal fun jsonTableFunctionColumnCount(functionName: String): Int = when (functionName) {
  "json_each", "jsonb_each", "json_each_text", "jsonb_each_text" -> 2
  else -> 1
}

/**
 * Whether the un-aliased form is valid. Only `*_object_keys` qualifies: Postgres names its column
 * after the function (no OUT-parameter name), so the function node serves as the column element.
 */
internal fun jsonTableFunctionAllowsNoAlias(functionName: String): Boolean = functionName == "json_object_keys" || functionName == "jsonb_object_keys"

/**
 * Fixed output column type of a JSON set-returning function by column position (the types do not
 * depend on the argument, unlike `generate_series`/`unnest`). `text`, `json` and `jsonb` all map to
 * Kotlin `String`:
 *  - `*_object_keys`             -> text
 *  - `*_array_elements`          -> json
 *  - `*_array_elements_text`     -> text
 *  - `json_each` / `jsonb_each`           -> (key text, value json)
 *  - `json_each_text` / `jsonb_each_text` -> (key text, value text)
 */
internal fun jsonTableFunctionRowType(functionName: String, columnIndex: Int): IntermediateType = when (functionName) {
  "json_object_keys", "jsonb_object_keys" -> IntermediateType(TEXT)
  "json_array_elements", "jsonb_array_elements" -> IntermediateType(PostgreSqlType.JSON)
  "json_array_elements_text", "jsonb_array_elements_text" -> IntermediateType(TEXT)
  "json_each", "jsonb_each" -> if (columnIndex == 0) IntermediateType(TEXT) else IntermediateType(PostgreSqlType.JSON)
  "json_each_text", "jsonb_each_text" -> IntermediateType(TEXT)
  else -> error("Unknown json table function $functionName")
}
