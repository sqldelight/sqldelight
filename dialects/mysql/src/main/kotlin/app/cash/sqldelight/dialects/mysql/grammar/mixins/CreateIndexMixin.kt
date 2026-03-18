package app.cash.sqldelight.dialects.mysql.grammar.mixins

import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlCreateIndexStmt
import com.alecstrong.sql.psi.core.psi.impl.SqlCreateIndexStmtImpl
import com.intellij.lang.ASTNode

internal abstract class CreateIndexMixin(node: ASTNode) :
  SqlCreateIndexStmtImpl(node),
  MySqlCreateIndexStmt
