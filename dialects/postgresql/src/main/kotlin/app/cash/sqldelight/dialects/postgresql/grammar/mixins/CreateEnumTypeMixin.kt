package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialect.api.PreCreateTableInitialization
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlCreateEnumTypeStmt
import com.alecstrong.sql.psi.core.psi.Schema
import com.alecstrong.sql.psi.core.psi.SchemaContributor
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode

/**
 * Usage `CREATE TYPE ABC AS ENUM ('a', 'b', 'c')`
 * Note: `IF NOT EXISTS` or `REPLACE` is not supported by PostgreSQL and means that `CREATE TYPE ...` is not idempotent.
 * PreCreateTableInitialization is used to ensure that `create type` is ordered before `create table` when using `.sq` files.
 * SchemaContributor is used to compile the `create type` in the `schema.create` code block.
 */
internal abstract class CreateEnumTypeMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  PostgreSqlCreateEnumTypeStmt,
  PreCreateTableInitialization,
  SchemaContributor {

  override fun modifySchema(schema: Schema) {
  }

  override fun name(): String {
    return typeIdentifier.text
  }
}
