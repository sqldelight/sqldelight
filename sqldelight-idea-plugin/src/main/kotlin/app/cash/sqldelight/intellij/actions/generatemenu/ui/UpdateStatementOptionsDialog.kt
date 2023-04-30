package app.cash.sqldelight.intellij.actions.generatemenu.ui

import app.cash.sqldelight.intellij.actions.generatemenu.StatementGenerationContext
import app.cash.sqldelight.intellij.actions.generatemenu.UpdateStatementOptions
import app.cash.sqldelight.intellij.actions.generatemenu.getPrimaryKeyIndices
import app.cash.sqldelight.intellij.actions.generatemenu.ui.support.replaceAll
import app.cash.sqldelight.intellij.actions.generatemenu.ui.views.TableSelectionWithDualColumnListView
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.openapi.project.Project

class UpdateStatementOptionsDialog(
  project: Project,
  modelData: StatementGenerationContext
) :
  StatementCreationOptionsDialog<TableSelectionWithDualColumnListView>(
    project,
    "Generate Update Statement",
    TableSelectionWithDualColumnListView(),
    modelData
  )
{


  private val updateColumnList = view.leftColumnList
  private val updateColumnListModel = view.leftColumnListModel
  private val whereColumnList = view.rightColumnList
  private val whereColumnListModel = view.rightColumnListModel

  init {
    init()
  }

  fun getUpdateStatementOptions(): UpdateStatementOptions {
    return UpdateStatementOptions(
      selectedCreateStatement.name(),
      updateColumnList.selectedValuesList ?: emptyList(),
      whereColumnList.selectedValuesList ?: emptyList()
    )
  }

  override fun initializeUIState() {
    view.menuLabel.text = "Select from:"
    view.leftColumnLabel.text = "Columns to update:"
    view.rightColumnLabel.text = "Where columns:"
    setDefaultSelections(selectedCreateStatement)
  }

  override fun onTableSelectionChanged(createTableStatement: SqlCreateTableStmt) {
    setDefaultSelections(createTableStatement)

  }

  /**
   * Updates the column lists' items and set the defaults selections for the given table
   */
  private fun setDefaultSelections(
    createStatement: SqlCreateTableStmt
  ) {
    updateColumnListModel.replaceAll(
      createStatement.columnDefList.filter { columnDef ->
        !columnDef.columnConstraintList.any { columnConstraint ->
          columnConstraint.textMatches("PRIMARY KEY")
        }
      }.map { it.columnName.name }
    )

    updateColumnList.addSelectionInterval(0, updateColumnListModel.size - 1)

    whereColumnListModel.replaceAll(
      createStatement.columnDefList.map { it.columnName.name }
    )
    createStatement.getPrimaryKeyIndices().forEach { index ->
      whereColumnList.addSelectionInterval(index, index)
    }
  }

}
