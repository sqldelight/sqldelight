package app.cash.sqldelight.core.lang.util

import app.cash.sqldelight.dialect.api.Validator
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr

internal object AnsiSqlValidator : Validator {
  override fun SqlFunctionExpr.validateFunction(annotationHolder: SqlAnnotationHolder) {}
}
