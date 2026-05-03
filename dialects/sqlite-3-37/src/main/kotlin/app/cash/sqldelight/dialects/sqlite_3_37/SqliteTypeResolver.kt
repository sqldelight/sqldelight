package app.cash.sqldelight.dialects.sqlite_3_37

import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialect.api.TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_35.SqliteTypeResolver as Sqlite335TypeResolver
import app.cash.sqldelight.dialects.sqlite_3_37.grammar.psi.SqliteTypes as SqliteTypes337
import com.alecstrong.sql.psi.core.psi.SqlColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes

/*
 * This class extends 3_35 SqliteTypeResolver as we need to call the inherited resolvers for previous dialects
 *
 * Strict Table Rules:
 * INTEGER PRIMARY KEY column, SQLite STRICT Table still accepts nullable.
 * Columns in a table-level composite PRIMARY KEY, SQlite STRICT table are non-nullable.
 * see https://www.sqlite.org/stricttables.html
 * */
open class SqliteTypeResolver(parentResolver: TypeResolver) : Sqlite335TypeResolver(parentResolver) {

  override fun simplifyType(intermediateType: IntermediateType): IntermediateType {
    val simplifiedType = super.simplifyType(intermediateType)

    val columnDef = simplifiedType.column ?: return simplifiedType
    val tableDef = columnDef.parent as? SqlCreateTableStmt ?: return simplifiedType

    val isStrict = tableDef.tableOptions?.tableOptionList?.any { it.node.findChildByType(SqliteTypes337.STRICT) != null } == true

    if (!isStrict) return simplifiedType

    val columnName = columnDef.columnName.name

    val isColumnPrimaryKey = columnDef.columnConstraintList.any {
      it.node.findChildByType(SqlTypes.PRIMARY) != null &&
        it.node.findChildByType(SqlTypes.KEY) != null
    }

    if (isColumnPrimaryKey) {
      return simplifiedType.nullableIf(simplifiedType.dialectType == PrimitiveType.INTEGER)
    }

    val isTablePrimaryKey = tableDef.tableConstraintList
      .filter {
        it.node.findChildByType(SqlTypes.PRIMARY) != null &&
          it.node.findChildByType(SqlTypes.KEY) != null
      }
      .any { tableConstraint ->
        columnName in tableConstraint.indexedColumnList.mapNotNull { indexedColumn ->
          val expr = indexedColumn.expr
          if (expr is SqlColumnExpr) expr.columnName.name else null
        }
      }

    return simplifiedType.nullableIf(!isTablePrimaryKey)
  }
}
