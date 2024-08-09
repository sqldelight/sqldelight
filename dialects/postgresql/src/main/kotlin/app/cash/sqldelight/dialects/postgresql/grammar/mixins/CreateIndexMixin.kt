package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlCreateIndexStmt
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.impl.SqlCreateIndexStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
/**
 * Storage parameter list 'autosummarize' | 'buffering' | 'deduplicate_items' | 'fastupdate' | 'fillfactor' | 'gin_pending_list_limit' | 'pages_per_range'
 * btree, hash, gist = [fillfactor (10-100) ]
 * btree = [deduplicate_items (0|1|on|off|true|false)]
 * gist = [buffering (auto|on|off)]
 * gin = [fastupdate (on|off|true|false), gin_pending_list_limit (64-2147483647) ]
 * brin = [autosummarize (on|off|true|false), pages_per_range (1-2147483647) ]
 */
internal abstract class CreateIndexMixin(node: ASTNode) :
  SqlCreateIndexStmtImpl(node),
  PostgreSqlCreateIndexStmt {

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    withStorageParameter?.let { wsp ->
      wsp.storageParametersList.zip(wsp.storageParameterList).forEach { sp ->
        indexMethod?.let { im ->
          when (im.text.lowercase()) {
            "brin" -> when (sp.first.text) {
              "autosummarize" -> autoSummarize(sp.second, annotationHolder)
              "pages_per_range" -> pagesPerRange(sp.second, annotationHolder)
              else -> unrecongizedParameter(sp.first, annotationHolder)
            }
            "btree" -> when (sp.first.text) {
              "fillfactor" -> fillFactor(sp.second, annotationHolder)
              "deduplicate_items" -> deduplicateItems(sp.second, annotationHolder)
              else -> unrecongizedParameter(sp.first, annotationHolder)
            }
            "gin" -> when (sp.first.text) {
              "fastupdate" -> fastUpdate(sp.second, annotationHolder)
              "gin_pending_list_limit" -> ginPendingListLimit(sp.second, annotationHolder)
              else -> unrecongizedParameter(sp.first, annotationHolder)
            }
            "gist" -> when (sp.first.text) {
              "fillfactor" -> fillFactor(sp.second, annotationHolder)
              "buffering" -> buffering(sp.second, annotationHolder)
              else -> unrecongizedParameter(sp.first, annotationHolder)
            }
            "hash" -> when (sp.first.text) {
              "fillfactor" -> fillFactor(sp.second, annotationHolder)
              else -> unrecongizedParameter(sp.first, annotationHolder)
            }
          }
        }
      }
    }
    super.annotate(annotationHolder)
  }

  companion object {

    private val pgBooleans = listOf("1", "0", "on", "off", "true", "false")

    fun autoSummarize(input: PsiElement, annotationHolder: SqlAnnotationHolder) {
      input.text.let { value ->
        if (value.lowercase() !in pgBooleans) {
          annotationHolder.createErrorAnnotation(
            input,
            """invalid value for boolean option "autosummarize" $value""",
          )
        }
      }
    }

    fun buffering(input: PsiElement, annotationHolder: SqlAnnotationHolder) {
      input.text.let { value ->
        if (value.lowercase() !in listOf("auto", "on", "off")) {
          annotationHolder.createErrorAnnotation(
            input,
            """invalid value for enum option "buffering" $value""",
          )
        }
      }
    }

    fun deduplicateItems(input: PsiElement, annotationHolder: SqlAnnotationHolder) {
      input.text.let { value ->
        if (value.lowercase() !in pgBooleans) {
          annotationHolder.createErrorAnnotation(
            input,
            """invalid value for boolean option "deduplicate_items" $value""",
          )
        }
      }
    }

    fun fastUpdate(input: PsiElement, annotationHolder: SqlAnnotationHolder) {
      input.text.let { value ->
        if (value.lowercase() !in pgBooleans) {
          annotationHolder.createErrorAnnotation(
            input,
            """invalid value for boolean option "fastupdate" $value""",
          )
        }
      }
    }

    fun fillFactor(input: PsiElement, annotationHolder: SqlAnnotationHolder) {
      input.text.toInt().let { value ->
        if (value !in 10..100) {
          annotationHolder.createErrorAnnotation(
            input,
            """value $value out of bounds for option "fillfactor"""",
          )
        }
      }
    }

    fun ginPendingListLimit(input: PsiElement, annotationHolder: SqlAnnotationHolder) {
      input.text.toInt().let { value ->
        if (value !in 64..Int.MAX_VALUE) {
          annotationHolder.createErrorAnnotation(
            input,
            """value $value out of bounds for option "gin_pending_list_limit"""",
          )
        }
      }
    }

    fun pagesPerRange(input: PsiElement, annotationHolder: SqlAnnotationHolder) {
      input.text.toInt().let { value ->
        if (value !in 1..Int.MAX_VALUE) {
          annotationHolder.createErrorAnnotation(
            input,
            """value $value out of bounds for option "pages_per_range"""",
          )
        }
      }
    }

    fun unrecongizedParameter(input: PsiElement, annotationHolder: SqlAnnotationHolder) {
      input.text.let { parameter ->
        annotationHolder.createErrorAnnotation(
          input,
          """unrecognized parameter "$parameter"""",
        )
      }
    }
  }
}
