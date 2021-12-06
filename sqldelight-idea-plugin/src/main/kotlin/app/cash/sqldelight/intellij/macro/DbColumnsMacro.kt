package app.cash.sqldelight.intellij.macro

import app.cash.sqldelight.intellij.SqlDelightLiveTemplateContextType
import com.alecstrong.sql.psi.core.SqlFileBase
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.ListResult
import com.intellij.codeInsight.template.Result
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.codeInsight.template.TextResult
import com.intellij.codeInsight.template.macro.MacroBase
import com.intellij.psi.PsiDocumentManager

class DbColumnsMacro : MacroBase("dbColumns", "dbColumns(tableName)") {

  override fun isAcceptableInContext(context: TemplateContextType?): Boolean {
    return context is SqlDelightLiveTemplateContextType
  }

  override fun calculateResult(
    params: Array<out Expression>,
    context: ExpressionContext,
    quick: Boolean
  ): Result? {
    if (params.isEmpty()) {
      return null
    }
    val expression = params.first()
    val tableName = expression.calculateResult(context) ?: return null
    val document = context.editor?.document ?: return null
    val psiFile = PsiDocumentManager.getInstance(context.project)
      .getPsiFile(document) as? SqlFileBase ?: return null
    val tables = psiFile.tables(true)
    val table = tables.firstOrNull { it.tableName.name == tableName.toString() } ?: return null
    val columns = table.query.columns
      .map { TextResult(it.element.text) }
    return ListResult(columns)
  }
}
