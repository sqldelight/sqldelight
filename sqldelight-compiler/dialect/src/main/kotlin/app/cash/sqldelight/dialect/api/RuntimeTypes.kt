package app.cash.sqldelight.dialect.api

import com.squareup.kotlinpoet.ClassName

/**
 * Types that can be specified by each dialect for different driver components
 */
data class RuntimeTypes(
  val cursorType: ClassName,
  val preparedStatementType: ClassName,
)
