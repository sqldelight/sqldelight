package app.cash.sqldelight.dialects.sqlite_3_35.grammar.mixins

import app.cash.sqldelight.dialects.sqlite_3_35.grammar.psi.SqliteReturningClause
import com.alecstrong.sql.psi.core.ModifiableFileLazy
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.FromQuery
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.alecstrong.sql.psi.core.psi.SqlDeleteStmt
import com.alecstrong.sql.psi.core.psi.SqlDeleteStmtLimited
import com.alecstrong.sql.psi.core.psi.SqlInsertStmt
import com.alecstrong.sql.psi.core.psi.SqlResultColumn
import com.alecstrong.sql.psi.core.psi.SqlUpdateStmt
import com.alecstrong.sql.psi.core.psi.SqlUpdateStmtLimited
import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil

internal abstract class ReturningClauseMixin(
  node: ASTNode
) : SqlCompositeElementImpl(node),
  SqliteReturningClause,
  FromQuery {
  private val queryExposed = ModifiableFileLazy {
    listOf(
      QueryElement.QueryResult(
        null,
        PsiTreeUtil.findChildrenOfType(this, SqlResultColumn::class.java)
          .flatMap { it.queryExposed().flatMap(QueryElement.QueryResult::columns) }
      )
    )
  }

  override fun queryExposed() = queryExposed.forFile(containingFile)

  override fun fromQuery(): Collection<QueryElement.QueryResult> {
    val tableName = when (val parent = parent) {
      is SqlInsertStmt -> parent.tableName
      is SqlDeleteStmt -> parent.qualifiedTableName?.tableName
      is SqlDeleteStmtLimited -> parent.qualifiedTableName?.tableName
      is SqlUpdateStmt -> parent.qualifiedTableName.tableName
      is SqlUpdateStmtLimited -> parent.qualifiedTableName.tableName
      else -> return emptyList()
    } ?: return emptyList()
    return tableAvailable(this, tableName.name)
  }

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    if (fromQuery().isEmpty()) {
      annotationHolder.createErrorAnnotation(this, "Could not find the table to select from.")
    }
  }
}
