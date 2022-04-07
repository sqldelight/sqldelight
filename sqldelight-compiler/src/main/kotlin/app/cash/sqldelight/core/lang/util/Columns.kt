package app.cash.sqldelight.core.lang.util

import com.alecstrong.sql.psi.core.psi.AliasElement
import com.alecstrong.sql.psi.core.psi.NamedElement
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.mixins.ColumnDefMixin

fun NamedElement.columnDefSource(): ColumnDefMixin? {
  if (this.parent is ColumnDefMixin) return this.parent as ColumnDefMixin
  if (this is AliasElement) return (source() as NamedElement).columnDefSource()
  if (this is SqlColumnName) return (reference!!.resolve() as NamedElement).columnDefSource()
  return null
}