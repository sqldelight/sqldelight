package app.cash.sqldelight.dialects.sqlite.json.module.grammar.mixins

import app.cash.sqldelight.dialect.api.ExposableType
import app.cash.sqldelight.dialect.api.IntermediateType
import app.cash.sqldelight.dialect.api.PrimitiveType
import app.cash.sqldelight.dialects.sqlite.json.module.grammar.JsonParser
import app.cash.sqldelight.dialects.sqlite.json.module.grammar.psi.SqliteJsonTableOrSubquery
import com.alecstrong.sql.psi.core.ModifiableFileLazy
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryResult
import com.alecstrong.sql.psi.core.psi.QueryElement.SynthesizedColumn
import com.alecstrong.sql.psi.core.psi.SqlExpr
import com.alecstrong.sql.psi.core.psi.SqlJoinClause
import com.alecstrong.sql.psi.core.psi.SqlNamedElementImpl
import com.alecstrong.sql.psi.core.psi.SqlTableName
import com.alecstrong.sql.psi.core.psi.impl.SqlTableOrSubqueryImpl
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.PsiElement

internal abstract class TableOrSubqueryMixin(node: ASTNode?) : SqlTableOrSubqueryImpl(node), SqliteJsonTableOrSubquery {
  private val queryExposed = ModifiableFileLazy lazy@{
    if (jsonFunctionName != null) {
      return@lazy listOf(QueryResult(
        table = jsonFunctionName!!,
        columns = emptyList(),
        synthesizedColumns = listOf(
          SynthesizedColumn(jsonFunctionName!!, acceptableValues = listOf("key", "value", "type", "atom", "id", "parent", "fullkey", "path", "json", "root"))
        )
      ))
    }
    super.queryExposed()
  }

  override fun queryExposed() = queryExposed.forFile(containingFile)

  override fun queryAvailable(child: PsiElement): Collection<QueryResult> {
    if (child is SqlExpr) {
      val parent = parent as SqlJoinClause
      return parent.tableOrSubqueryList.takeWhile { it != this }.flatMap { it.queryExposed() }
    }
    return super.queryAvailable(child)
  }
}

internal abstract class JsonFunctionNameMixin(node: ASTNode): SqlNamedElementImpl(node), SqlTableName, ExposableType {
  override fun getId(): PsiElement? = null
  override fun getString(): PsiElement? = null
  override val parseRule: (PsiBuilder, Int) -> Boolean = JsonParser::json_function_name_real
  override fun type() = IntermediateType(PrimitiveType.TEXT)
}