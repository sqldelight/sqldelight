package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement

interface ExposableType : SqlAnnotatedElement {
  fun type(): IntermediateType
}