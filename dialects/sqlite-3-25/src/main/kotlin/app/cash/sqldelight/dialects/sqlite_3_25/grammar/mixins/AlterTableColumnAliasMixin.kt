package app.cash.sqldelight.dialects.sqlite_3_25.grammar.mixins

import app.cash.sqldelight.dialects.sqlite_3_25.grammar.SqliteParser
import app.cash.sqldelight.dialects.sqlite_3_25.grammar.psi.SqliteAlterTableRenameColumn
import com.alecstrong.sql.psi.core.psi.SqlColumnAlias
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.impl.SqlColumnAliasImpl
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.util.PsiTreeUtil

internal abstract class AlterTableColumnAliasMixin(
  node: ASTNode,
) : SqlColumnAliasImpl(node),
  SqlColumnAlias {
  override val parseRule: (PsiBuilder, Int) -> Boolean = SqliteParser::alter_table_column_alias_real

  override fun source() = PsiTreeUtil.getChildOfType(parent as SqliteAlterTableRenameColumn, SqlColumnName::class.java)!!
}
