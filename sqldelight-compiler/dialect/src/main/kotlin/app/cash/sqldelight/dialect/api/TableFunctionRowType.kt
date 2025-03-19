package app.cash.sqldelight.dialect.api

import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement
import com.alecstrong.sql.psi.core.psi.SqlTypeName

interface TableFunctionRowType : SqlAnnotatedElement {
  fun columnType(): SqlTypeName
}
