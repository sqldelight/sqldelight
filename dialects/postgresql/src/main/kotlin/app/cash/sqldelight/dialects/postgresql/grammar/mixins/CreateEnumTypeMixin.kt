package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialect.api.PreCreateTableInitialization
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlCreateEnumTypeStmt
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode

internal abstract class CreateEnumTypeMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  PostgreSqlCreateEnumTypeStmt,
  PreCreateTableInitialization
