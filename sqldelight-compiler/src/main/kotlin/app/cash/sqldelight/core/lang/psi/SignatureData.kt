package app.cash.sqldelight.core.lang.psi

import app.cash.sqldelight.core.lang.util.type
import com.alecstrong.sql.psi.core.psi.QueryElement.QueryColumn
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.ParameterAdditionalData

fun QueryColumn.parameterValue(): SuggestedRefactoringSupport.Parameter? = element.type().let { type ->
  val column = type.column ?: return null
  SuggestedRefactoringSupport.Parameter(
    id = type.name,
    name = type.name,
    type = column.columnType.typeName.text,
    additionalData = column.columnConstraintList.takeIf { it.isNotEmpty() }?.let { list ->
      ColumnConstraints((list.filter { it.text.isNotBlank() }.joinToString(" ") { it.text.trim() }))
    }
  )
}

fun SqlColumnDef.asParameter() = SuggestedRefactoringSupport.Parameter(
  id = columnName.name,
  name = columnName.name,
  type = columnType.typeName.text,
  additionalData = columnConstraintList.takeIf { it.isNotEmpty() }?.let { list ->
    ColumnConstraints((list.filter { it.text.isNotBlank() }.joinToString(" ") { it.text.trim() }))
  }
)

data class ColumnConstraints(val constraints: String) : ParameterAdditionalData {
  override fun toString(): String {
    return constraints
  }
}
