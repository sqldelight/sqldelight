package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.alecstrong.sql.psi.core.SqlSchemaContributorElementType
import com.alecstrong.sql.psi.core.psi.QueryElement
import com.alecstrong.sql.psi.core.psi.SchemaContributorStub
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.TableElement
import com.alecstrong.sql.psi.core.psi.impl.SqlCreateViewStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

/**
 * See sql-psi com.alecstrong.sql.psi.core.psi.mixins.CreateViewMixin where `REPLACE` is enabled
 * Add annotations to check replace has an identical set of columns in same order, but allows appending columns
 */
abstract class CreateOrReplaceViewMixin : SqlCreateViewStmtImpl {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SchemaContributorStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

  override fun annotate(annotationHolder: SqlAnnotationHolder) {
    val currentColumns: List<QueryElement.QueryColumn> = tableAvailable(this, viewName.name).flatMap { it.columns }
    val newColumns = compoundSelectStmt!!.queryExposed().flatMap { it.columns }
    if (currentColumns.size > newColumns.size) {
      annotationHolder.createErrorAnnotation(this, "Cannot drop columns from ${viewName.name}")
    }

    currentColumns.zip(newColumns).firstOrNull { (current, new) -> current != new }?.let { (current, new) ->
      annotationHolder.createErrorAnnotation(this, """Cannot change name of view column "${current.element.text}" to "${new.element.text}"""")
    }
    super.annotate(annotationHolder)
  }
}

class CreateViewElementType(name: String) : SqlSchemaContributorElementType<TableElement>(name, TableElement::class.java) {
  override fun nameType() = SqlTypes.VIEW_NAME
  override fun createPsi(stub: SchemaContributorStub) = SqlCreateViewStmtImpl(stub, this)
}
