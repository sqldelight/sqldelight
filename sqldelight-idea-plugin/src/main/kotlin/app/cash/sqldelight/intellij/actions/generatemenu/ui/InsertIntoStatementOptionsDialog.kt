package app.cash.sqldelight.intellij.actions.generatemenu.ui

import app.cash.sqldelight.intellij.actions.generatemenu.InsertStatementOptions
import app.cash.sqldelight.intellij.actions.generatemenu.StatementGenerationContext
import app.cash.sqldelight.intellij.actions.generatemenu.getPrimaryKeyIndices
import app.cash.sqldelight.intellij.actions.generatemenu.ui.support.replaceAll
import app.cash.sqldelight.intellij.actions.generatemenu.ui.views.TableSelectionWithSingleColumnListView
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.openapi.project.Project

class InsertIntoStatementOptionsDialog(
  project: Project,
  private val modelData: StatementGenerationContext
) :
  StatementCreationOptionsDialog<TableSelectionWithSingleColumnListView>(
    project,
    "Generate Insert Statement",
    TableSelectionWithSingleColumnListView(),
    modelData
  )
{

  init {
    init()
  }

  fun getInsertStatementOptions(): InsertStatementOptions {
    return InsertStatementOptions(
      selectedCreateStatement,
      selectedColumns = view.columnList.selectedValuesList ?: emptyList(),
    )
  }

  override fun initializeUIState() {
    view.menuLabel.text = "Insert into:"

    val activeIndex = modelData.activeIndex
    val createTableStatements = modelData.createTableStatements

    setDefaultSelections(createTableStatements[activeIndex])
  }

  override fun onTableSelectionChanged(createTableStatement: SqlCreateTableStmt) {
    setDefaultSelections(createTableStatement)
  }

  /**
   * Updates the column list's items and set the defaults selections for the given table
   */
  private fun setDefaultSelections(selectedCreateStatement: SqlCreateTableStmt) {
    this.view.columnListModel.replaceAll(selectedCreateStatement.columnDefList.map { it.columnName.name })

    val columnDefList = selectedCreateStatement.columnDefList
    view.columnList.addSelectionInterval(0, columnDefList.size - 1)

    selectedCreateStatement.getPrimaryKeyIndices().forEach { index ->
      view.columnList.removeSelectionInterval(index, index)
    }
  }

}