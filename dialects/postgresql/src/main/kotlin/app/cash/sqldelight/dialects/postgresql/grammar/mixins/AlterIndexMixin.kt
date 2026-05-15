package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.mixins.CreateIndexMixin.Companion.autoSummarize
import app.cash.sqldelight.dialects.postgresql.grammar.mixins.CreateIndexMixin.Companion.buffering
import app.cash.sqldelight.dialects.postgresql.grammar.mixins.CreateIndexMixin.Companion.deduplicateItems
import app.cash.sqldelight.dialects.postgresql.grammar.mixins.CreateIndexMixin.Companion.fastUpdate
import app.cash.sqldelight.dialects.postgresql.grammar.mixins.CreateIndexMixin.Companion.fillFactor
import app.cash.sqldelight.dialects.postgresql.grammar.mixins.CreateIndexMixin.Companion.ginPendingListLimit
import app.cash.sqldelight.dialects.postgresql.grammar.mixins.CreateIndexMixin.Companion.pagesPerRange
import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlAlterIndexStmt
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlCompositeElementImpl
import com.intellij.lang.ASTNode

/**
 * Validates storage parameters for alter index
 * see app.cash.sqldelight.dialects.postgresql.grammar.mixins.CreateIndexMixin
 *
 * AlterIndexMixin doesn't implement SchemaContributor as alterIndex is not supported by sql-psi.
 */
internal abstract class AlterIndexMixin(node: ASTNode) :
  SqlCompositeElementImpl(node),
  PostgreSqlAlterIndexStmt {

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    setStorageParameters?.let { ssp ->
      ssp.storageParametersList.zip(ssp.storageParameterList).forEach { sp ->
        when (sp.first.text.lowercase()) {
          "autosummarize" -> autoSummarize(sp.second, annotationHolder)
          "buffering" -> buffering(sp.second, annotationHolder)
          "deduplicate_items" -> deduplicateItems(sp.second, annotationHolder)
          "fastupdate" -> fastUpdate(sp.second, annotationHolder)
          "fillfactor" -> fillFactor(sp.second, annotationHolder)
          "gin_pending_list_limit" -> ginPendingListLimit(sp.second, annotationHolder)
          "pages_per_range" -> pagesPerRange(sp.second, annotationHolder)
        }
      }
    }
  }
}
