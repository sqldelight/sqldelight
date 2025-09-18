package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialect.api.PreCreateTableInitialization
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlDropTypeStmt
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode

internal abstract class DropEnumTypeMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  PostgreSqlDropTypeStmt,
  PreCreateTableInitialization
