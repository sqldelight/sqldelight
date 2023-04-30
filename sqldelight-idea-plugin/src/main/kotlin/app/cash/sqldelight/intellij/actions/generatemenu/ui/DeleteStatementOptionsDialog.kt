package app.cash.sqldelight.intellij.actions.generatemenu.ui

import app.cash.sqldelight.intellij.actions.generatemenu.DeleteStatementOptions
import app.cash.sqldelight.intellij.actions.generatemenu.StatementGenerationContext
import app.cash.sqldelight.intellij.actions.generatemenu.getPrimaryKeyIndices
import app.cash.sqldelight.intellij.actions.generatemenu.ui.support.replaceAll
import app.cash.sqldelight.intellij.actions.generatemenu.ui.views.TableSelectionWithSingleColumnListView
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.openapi.project.Project

class DeleteStatementOptionsDialog(
  project: Project,
  modelData: StatementGenerationContext
) :
  StatementCreationOptionsDialog<TableSelectionWithSingleColumnListView>(
    project,
    "Generate Delete Statement",
    TableSelectionWithSingleColumnListView(),
    modelData
  )
{

  init {
    init()
  }

  fun getDeleteStatementOptions(): DeleteStatementOptions {
    return DeleteStatementOptions(
      selectedCreateStatement.name(),
      whereColumns = view.columnList.selectedValuesList ?: emptyList(),
    )
  }

  override fun initializeUIState() {
    view.menuLabel.text = "Delete from:"
    setDefaultSelections(selectedCreateStatement)
  }

  override fun onTableSelectionChanged(createTableStatement: SqlCreateTableStmt) {
    setDefaultSelections(createTableStatement)
  }

  /**
   * Updates the column list's items and set the defaults selections for the given table
   */
  private fun setDefaultSelections(selectedCreateStatement: SqlCreateTableStmt) {
    this.view.columnListModel.replaceAll(
      selectedCreateStatement.columnDefList.map { it.columnName.name }
    )
    selectedCreateStatement.getPrimaryKeyIndices().forEach { index ->
      view.columnList.addSelectionInterval(index, index)
    }
  }

}