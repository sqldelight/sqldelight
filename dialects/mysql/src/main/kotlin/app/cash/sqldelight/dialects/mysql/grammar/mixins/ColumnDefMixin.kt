package app.cash.sqldelight.dialects.mysql.grammar.mixins

import com.alecstrong.sql.psi.core.psi.impl.SqlColumnDefImpl
import com.intellij.lang.ASTNode

internal class ColumnDefMixin(node: ASTNode) : SqlColumnDefImpl(node) {

  override fun hasDefaultValue(): Boolean {
    return columnConstraintList.any { it.node.text == "AUTO_INCREMENT" } || super.hasDefaultValue()
  }
}
