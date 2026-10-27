package app.cash.sqldelight.dialects.sqlite_3_24.grammar.mixins

import app.cash.sqldelight.dialects.sqlite_3_24.grammar.psi.SqliteInsertStmt
import app.cash.sqldelight.dialects.sqlite_3_24.grammar.psi.SqliteUpsertDoUpdate
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlBinaryAddExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryEqualityExpr
import com.alecstrong.sql.psi.core.psi.SqlBinaryExpr
import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlSetterExpression
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.SqlUpdateStmtSubsequentSetter
import com.alecstrong.sql.psi.core.psi.impl.SqlInsertStmtImpl
import com.alecstrong.sql.psi.core.psi.mixins.ColumnDefMixin
import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil

internal abstract class InsertStmtMixin(
  node: ASTNode,
) : SqlInsertStmtImpl(node),
  SqliteInsertStmt {
  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    super.annotate(annotationHolder)
    val insertDefaultValues = insertStmtValues?.node?.findChildByType(
      SqlTypes.DEFAULT,
    ) != null

    upsertClause?.let { upsert ->
      val upsertDoUpdate = upsert.upsertDoUpdate
      if (insertDefaultValues && upsertDoUpdate != null) {
        annotationHolder.createErrorAnnotation(upsert, "The upsert clause is not supported after DEFAULT VALUES")
      }

      val insertOr = node.findChildByType(
        SqlTypes.INSERT,
      )?.treeNext
      val replace = node.findChildByType(
        SqlTypes.REPLACE,
      )
      val conflictResolution = when {
        replace != null -> SqlTypes.REPLACE
        insertOr != null && insertOr.elementType == SqlTypes.OR -> {
          val type = insertOr.treeNext.elementType
          check(
            type == SqlTypes.ROLLBACK ||
              type == SqlTypes.ABORT ||
              type == SqlTypes.FAIL ||
              type == SqlTypes.IGNORE,
          )
          type
        }
        else -> null
      }

      if (conflictResolution != null && upsertDoUpdate != null) {
        annotationHolder.createErrorAnnotation(
          upsertDoUpdate,
          "Cannot use DO UPDATE while " +
            "also specifying a conflict resolution algorithm ($conflictResolution)",
        )
      }

      if (upsertDoUpdate != null) {
        validateOptimisticLock(upsertDoUpdate, annotationHolder)
      }
    }
  }

  private fun validateOptimisticLock(
    doUpdate: SqliteUpsertDoUpdate,
    annotationHolder: SqlAnnotationHolder,
  ) {
    val table = tablesAvailable(this)
      .firstOrNull { it.tableName.name == tableName.name } ?: return
    val lock = table.query.columns
      .mapNotNull { it.element.parent as? ColumnDefMixin }
      .singleOrNull { col ->
        col.columnType.node.getChildren(null).any { it.text == "LOCK" }
      } ?: return

    val firstColumnName = PsiTreeUtil.getChildrenOfTypeAsList(doUpdate, SqlColumnName::class.java).firstOrNull()
    val firstSetterExpr = PsiTreeUtil.getChildrenOfTypeAsList(doUpdate, SqlSetterExpression::class.java).firstOrNull()
    val subsequentSetters = PsiTreeUtil.getChildrenOfTypeAsList(doUpdate, SqlUpdateStmtSubsequentSetter::class.java)

    val columns = listOfNotNull(firstColumnName) + subsequentSetters.mapNotNull { it.columnName }
    val setters = listOfNotNull(firstSetterExpr) + subsequentSetters.mapNotNull { it.setterExpression }

    val setter = columns.zip(setters)
      .singleOrNull { (col, _) -> col.textMatches(lock.columnName.name) }
      ?.second

    if (setter == null) {
      annotationHolder.createErrorAnnotation(
        doUpdate,
        "This statement is missing the optimistic lock in its SET clause.",
      )
      return
    }

    // excluded holds the row being inserted, so only the target table carries the stored lock.
    val storedNames = listOfNotNull(tableName.name, tableAlias?.name)
    fun SqlColumnExpr.isStoredLock() = columnName.textMatches(lock.columnName.name) &&
      (tableName?.let { qualifier -> qualifier.name in storedNames } ?: true)

    val setterExpressions = (setter.expr as? SqlBinaryExpr)?.getExprList()
    val selfIncrementsByOne = setter.expr is SqlBinaryAddExpr &&
      setter.expr.node.getChildren(null).any { it.text == "+" } &&
      setterExpressions?.getOrNull(1)?.textMatches("1") ?: false
    val bindIncrement = selfIncrementsByOne &&
      setterExpressions.getOrNull(0) is SqlBindExpr
    val selfIncrements = selfIncrementsByOne &&
      (setterExpressions.getOrNull(0) as? SqlColumnExpr)?.isStoredLock() == true

    // Confirm the statement has SET lock = :arg + 1 or SET lock = lock + 1
    if (!bindIncrement && !selfIncrements) {
      annotationHolder.createErrorAnnotation(
        doUpdate,
        """The optimistic lock must be set exactly like "${lock.columnName.name} = :${lock.columnName.name} + 1".""",
      )
      return
    }

    // Unlike a multirow UPDATE, DO UPDATE only touches the row that conflicted, so it still needs the WHERE clause.
    val whereExpr = PsiTreeUtil.getChildrenOfTypeAsList(doUpdate, SqlExpr::class.java).firstOrNull()
    if (whereExpr == null) {
      annotationHolder.createErrorAnnotation(
        doUpdate,
        "This statement is missing a WHERE clause to check the optimistic lock.",
      )
      return
    }

    val equalityExprs = buildList<SqlBinaryEqualityExpr> {
      if (whereExpr is SqlBinaryEqualityExpr) add(whereExpr)
      addAll(PsiTreeUtil.findChildrenOfType(whereExpr, SqlBinaryEqualityExpr::class.java))
    }
    if (equalityExprs.none { eq ->
        eq.node.getChildren(null).any { it.text == "=" || it.text == "==" } &&
          (eq.getExprList()[0] as? SqlColumnExpr)?.isStoredLock() == true &&
          eq.getExprList()[1] is SqlBindExpr
      }
    ) {
      annotationHolder.createErrorAnnotation(
        doUpdate,
        """The optimistic lock must be queried exactly like "${lock.columnName.name} == :${lock.columnName.name}".""",
      )
    }
  }
}
