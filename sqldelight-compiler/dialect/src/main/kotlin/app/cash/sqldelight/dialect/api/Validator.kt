package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr

interface Validator {
  /**
   * Validates the function
   */
  fun SqlFunctionExpr.validateFunction(annotationHolder: SqlAnnotationHolder) {}
}
