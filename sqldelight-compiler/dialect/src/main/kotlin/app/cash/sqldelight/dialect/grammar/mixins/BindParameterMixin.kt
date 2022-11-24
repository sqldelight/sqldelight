package app.cash.sqldelight.dialect.grammar.mixins

import com.alecstrong.sql.psi.core.psi.SqlBindParameter
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode

abstract class BindParameterMixin(node: ASTNode) : SqlCompositeElementImpl(node), SqlBindParameter {
  /**
   * Overwrite, if the user provided sql parameter should be overwritten by sqldelight with [replaceWith].
   *
   * Some sql dialects support other bind parameter besides `?`, but sqldelight should still replace the
   * user provided parameter with [replaceWith] for a homogen generated code.
   */
  open fun replaceWith(isAsync: Boolean, index: Int): String = "?"
}
