package com.squareup.sqldelight.intellij.macro

import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.Result
import com.intellij.codeInsight.template.TextResult
import com.intellij.codeInsight.template.macro.MacroBase
import com.intellij.psi.PsiDocumentManager
import com.squareup.sqldelight.core.lang.util.findChildrenOfType

class PkMacro : MacroBase("pk", "pk(tableName)") {

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

    val createTableStmt = psiFile.sqlStmtList?.findChildrenOfType<SqlCreateTableStmt>()
      .orEmpty()
      .firstOrNull { it.tableName.name == tableName.toString() } ?: return null

    val pk = createTableStmt.columnDefList.firstOrNull { columnDef ->
      columnDef.columnConstraintList.any { columnConstraint ->
        columnConstraint.textMatches("PRIMARY KEY")
      }
    }?.columnName
    return pk?.let { TextResult(it.name) }
  }
}
