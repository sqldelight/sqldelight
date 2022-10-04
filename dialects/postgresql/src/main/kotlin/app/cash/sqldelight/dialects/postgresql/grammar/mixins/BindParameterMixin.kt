package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialect.grammar.mixins.BindParameterMixin
import com.intellij.lang.ASTNode

abstract class BindParameterMixin(node: ASTNode) : BindParameterMixin(node) {
  override fun replaceWith(isAsync: Boolean, index: Int): String = when {
    isAsync -> "$$index"
    else -> "?"
  }
}
