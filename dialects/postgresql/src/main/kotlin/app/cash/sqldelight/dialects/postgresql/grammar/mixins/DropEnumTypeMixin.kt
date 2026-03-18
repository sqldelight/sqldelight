package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlDropTypeStmt
import com.alecstrong.sql.psi.core.psi.Schema
import com.alecstrong.sql.psi.core.psi.SchemaContributor
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode

/**
 * Usage `DROP TYPE [IF EXISTS] ABC CASCADE | RESTRICT`
 * Here, unlike PostgreSql, only a single type can be dropped per statement to keep a single typeIdentifier name.
 * PreCreateTableInitialization is not used as it likely depends on tables using the enum type.
 * SchemaContributor is used to include the `drop type` in the `schema.create` code block.
 */
internal abstract class DropEnumTypeMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  PostgreSqlDropTypeStmt,
  SchemaContributor {

  override fun modifySchema(schema: Schema) {
  }

  override fun name(): String {
    return typeIdentifier.text
  }
}
