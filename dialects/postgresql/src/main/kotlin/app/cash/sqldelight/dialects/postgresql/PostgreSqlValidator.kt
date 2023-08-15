package app.cash.sqldelight.dialects.postgresql

import app.cash.sqldelight.dialect.api.Validator
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlFunctionExpr

class PostgreSqlValidator(private val parentValidator: Validator) : Validator {
  override fun SqlFunctionExpr.validateFunction(
    annotationHolder: SqlAnnotationHolder,
  ) {
    when (functionName.text.lowercase()) {
      "max", "min" -> {
        if (exprList.size != 1) {
          annotationHolder.createErrorAnnotation(
            this,
            "${functionName.text} only takes one argument",
          )
        }
      }

      else -> with(parentValidator) {
        validateFunction(annotationHolder)
      }
    }
  }
}
