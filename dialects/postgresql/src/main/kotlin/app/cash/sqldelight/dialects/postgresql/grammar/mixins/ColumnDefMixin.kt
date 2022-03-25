package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlTypeName
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.impl.SqlColumnDefImpl
import com.intellij.lang.ASTNode

internal class ColumnDefMixin(node: ASTNode) : SqlColumnDefImpl(node), SqlColumnDef {

  override fun hasDefaultValue(): Boolean {
    return isSerial() || super.hasDefaultValue()
  }
}

private fun SqlColumnDef.isSerial(): Boolean {
  val typeName = columnType.typeName as PostgreSqlTypeName
  return typeName.smallSerialDataType != null || typeName.serialDataType != null || typeName.bigSerialDataType != null
}
