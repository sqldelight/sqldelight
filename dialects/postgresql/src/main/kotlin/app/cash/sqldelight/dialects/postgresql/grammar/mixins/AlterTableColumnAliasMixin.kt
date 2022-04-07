package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.PostgreSqlParser
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAlterTableRenameColumn
import com.alecstrong.sql.psi.core.psi.SqlColumnAlias
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.impl.SqlColumnAliasImpl
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.util.PsiTreeUtil

internal abstract class AlterTableColumnAliasMixin(
  node: ASTNode
) : SqlColumnAliasImpl(node),
  SqlColumnAlias {
  override val parseRule: (PsiBuilder, Int) -> Boolean = PostgreSqlParser::alter_table_column_alias_real

  override fun source() = PsiTreeUtil.getChildOfType(parent as PostgreSqlAlterTableRenameColumn, SqlColumnName::class.java)!!
}
