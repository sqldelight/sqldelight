package app.cash.sqldelight.dialects.postgresql.grammar.mixins

import app.cash.sqldelight.dialects.postgresql.grammar.psi.PostgreSqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryColumn
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.QueryElement.SynthesizedColumn
import com.alecstrong.sql.psi.core.psi.SchemaContributorStub
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.impl.SqlCreateTableStmtImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType

internal abstract class CreateTableMixin :
  SqlCreateTableStmtImpl,
  PostgreSqlCreateTableStmt {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SchemaContributorStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

  override fun queryAvailable(child: PsiElement): List<QueryResult> {
    val containsWithoutId = tableOptions
      ?.tableOptionList
      ?.any {
        (it.node.findChildByType(SqlTypes.WITHOUT) != null)
      } ?: false
    val synthesizedColumns = if (!containsWithoutId) {
      val columnNames = columnDefList.mapNotNull { it.columnName.name }
      listOf(
        SynthesizedColumn(
          table = this,
          acceptableValues = listOf("tableoid", "xmin", "cmin", "xmax", "cmax", "ctid").filter { it !in columnNames },
        ),
      )
    } else {
      emptyList()
    }
    return listOf(
      QueryResult(
        table = tableName,
        columns = columnDefList.map { it.columnName }.asColumns(),
        synthesizedColumns = synthesizedColumns,
      ),
    )
  }
}

private fun List<PsiElement>.asColumns() = map { QueryColumn(it) }
