package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.psi.SqlDelightColumnType
import com.alecstrong.sql.psi.core.psi.SqlBindExpr
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlInsertStmt
import com.alecstrong.sql.psi.core.psi.SqlInsertStmtValues
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.SqlValuesExpression
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.CreateParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoHandlerWithTabActionSupport
import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.ParameterInfoUtils
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType

class SqlDelightParameterInfoHandler : ParameterInfoHandlerWithTabActionSupport<SqlValuesExpression, List<String>, SqlBindExpr> {

  override fun couldShowInLookup(): Boolean {
    return false
  }

  override fun getParametersForLookup(
    item: LookupElement,
    context: ParameterInfoContext
  ): Array<Any> {
    return emptyArray()
  }

  override fun findElementForParameterInfo(context: CreateParameterInfoContext): SqlValuesExpression? {
    val element = context.file.findElementAt(context.offset)
    val valuesExpr = element?.parentOfType<SqlValuesExpression>() ?: return null
    if (valuesExpr.parent !is SqlInsertStmtValues) {
      return null
    }

    val columns = element.parentOfType<SqlInsertStmt>()?.columnNameList.orEmpty()
      .mapNotNull { it.reference?.resolve()?.parent as? SqlColumnDef }
      .mapNotNull { columnDef ->
        val columnType = PsiTreeUtil.getChildOfType(columnDef, SqlDelightColumnType::class.java) ?: return@mapNotNull null
        val annotations = columnType.annotationList.joinToString(", ") { "@${it.text}" }
        val columnName = columnDef.columnName.text
        val type = columnType.javaTypeName?.text ?: columnType.typeName.text
        buildString {
          if (annotations.isNotBlank()) {
            append(annotations)
            append(" ")
          }
          append("$columnName: $type")
        }
      }
    context.itemsToShow = arrayOf(columns)
    return valuesExpr
  }

  override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): SqlValuesExpression? {
    return context.file.findElementAt(context.offset)?.parentOfType()
  }

  override fun showParameterInfo(
    element: SqlValuesExpression,
    context: CreateParameterInfoContext
  ) {
    context.showHint(element, element.textRange.startOffset, this)
  }

  override fun updateParameterInfo(
    parameterOwner: SqlValuesExpression,
    context: UpdateParameterInfoContext
  ) {
    val offset = context.offset
    val index = ParameterInfoUtils.getCurrentParameterIndex(parameterOwner.node, offset, SqlTypes.COMMA)
    context.setCurrentParameter(index)
  }

  override fun updateUI(p: List<String>, context: ParameterInfoUIContext) {
    val text = p.joinToString(", ")
    val index = context.currentParameterIndex
    val s = p.getOrNull(index)
    var startIndex = -1
    var endIndex = -1
    if (s != null) {
      startIndex = text.indexOf(s)
      endIndex = startIndex + s.length
    }
    context.setupUIComponentPresentation(
      text,
      startIndex,
      endIndex,
      !context.isUIComponentEnabled,
      false,
      false,
      context.defaultParameterColor
    )
  }

  override fun getActualParameters(o: SqlValuesExpression): Array<SqlBindExpr> {
    return o.exprList
      .filterIsInstance<SqlBindExpr>()
      .toTypedArray()
  }

  override fun getActualParameterDelimiterType(): IElementType {
    return SqlTypes.COMMA
  }

  override fun getActualParametersRBraceType(): IElementType = SqlTypes.RP

  override fun getArgumentListAllowedParentClasses(): Set<Class<*>> {
    return emptySet()
  }

  override fun getArgListStopSearchClasses(): Set<Class<*>> {
    return emptySet()
  }

  override fun getArgumentListClass(): Class<SqlValuesExpression> {
    return SqlValuesExpression::class.java
  }
}
