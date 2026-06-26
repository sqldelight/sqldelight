package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
/**
 * A table function (set-returning function) whose output column type is resolved to an
 * IntermediateType, e.g. PostgreSQL `unnest(...)` (the array element type of  source column) or
 * `generate_series(...)` . The TypeResolver is supplied by the caller so dialect mixins can resolve types without
 * needing access into the compiler core api.
 */
interface TableFunctionExprRowType : SqlAnnotatedElement {
  fun rowType(typeResolver: TypeResolver): IntermediateType
}
