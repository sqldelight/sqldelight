package app.cash.sqldelight.intellij.actions.generatemenu.ui

import app.cash.sqldelight.intellij.actions.generatemenu.SelectQueryOptions
import app.cash.sqldelight.intellij.actions.generatemenu.StatementGenerationContext
import app.cash.sqldelight.intellij.actions.generatemenu.getPrimaryKeyIndices
import app.cash.sqldelight.intellij.actions.generatemenu.ui.support.replaceAll
import app.cash.sqldelight.intellij.actions.generatemenu.ui.views.TableSelectionWithDualColumnListView
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.openapi.project.Project

class SelectQueryOptionsDialog(
    project: Project,
    modelData: StatementGenerationContext
) :
  StatementCreationOptionsDialog<TableSelectionWithDualColumnListView>(
      project,
      "Generate Select Statement",
      TableSelectionWithDualColumnListView(),
      modelData
  )
{

  private val queryColumnList = view.leftColumnList
  private val queryColumnListModel = view.leftColumnListModel
  private val whereColumnList = view.rightColumnList
  private val whereColumnListModel = view.rightColumnListModel

  init {
    init()
  }

  fun getSelectQueryOptions(): SelectQueryOptions {
    return SelectQueryOptions(
        selectedCreateStatement.name(),
        queryColumnList.selectedValuesList ?: emptyList(),
        whereColumnList.selectedValuesList ?: emptyList()
    )
  }

  override fun initializeUIState() {
    view.menuLabel.text = "Select from:"
    setDefaultSelections(selectedCreateStatement)
  }

  override fun onTableSelectionChanged(createTableStatement: SqlCreateTableStmt) {
    setDefaultSelections(createTableStatement)
  }

  /**
   * Updates the column lists' items and set the defaults selections for the given table
   */
  private fun setDefaultSelections(createStatement: SqlCreateTableStmt) {
    // add all columns to the query list, and select them all
    queryColumnListModel.replaceAll(
      createStatement.columnDefList.map { it.columnName.name }
    )
    queryColumnList.addSelectionInterval(0, queryColumnListModel.size - 1)

    // add all columns to the where list, and select the primary keys
    whereColumnListModel.replaceAll(
      createStatement.columnDefList.map { it.columnName.name }
    )
    createStatement.getPrimaryKeyIndices().forEach { index ->
      whereColumnList.addSelectionInterval(index, index)
    }
  }

}