package app.cash.sqldelight.dialects.sqlite_3_18.grammar.mixins

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.psi.SqlCompositeElement
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.impl.SqlPragmaNameImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.tree.TokenSet

internal abstract class PragmaFunctionValidatorMixin(node: ASTNode) : SqlPragmaNameImpl(node) {
    private fun SqlCompositeElement.annotateReservedKeywords(annotationHolder: SqlAnnotationHolder) {
        children.filterIsInstance<SqlCompositeElement>().forEach {
            it.annotateReservedKeywords(annotationHolder)
        }
        node.getChildren(TokenSet.create(SqlTypes.ID)).forEach {
            if (it.text !in acceptedKeys) {
                annotationHolder.createErrorAnnotation(this, "not a valid pragma function")
            }
        }
    }
    override fun annotate(annotationHolder: SqlAnnotationHolder) {
        annotateReservedKeywords(annotationHolder)
        super.annotate(annotationHolder)
    }

    companion object {
        // This list is providing the names for all pragma functions that we should support.
        // For more information please look at the list of pragma https://www.sqlite.org/pragma.html#syntax
        private val acceptedKeys = setOf(
                "pragma_application_id",
                "pragma_cache_size",
                "pragma_cache_spill",
                "pragma_compile_options",
                "pragma_count_changes",
                "pragma_cell_size_check",
                "pragma_freelist_count",
                "pragma_max_page_count",
                "pragma_page_size",
                "pragma_page_count",
                "pragma_user_version",
        )
    }
}