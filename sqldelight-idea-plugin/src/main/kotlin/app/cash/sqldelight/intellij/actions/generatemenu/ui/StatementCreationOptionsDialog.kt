package app.cash.sqldelight.intellij.actions.generatemenu.ui

import app.cash.sqldelight.intellij.actions.generatemenu.StatementGenerationContext
import app.cash.sqldelight.intellij.actions.generatemenu.ui.views.TableContextView
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComboBox
import javax.swing.JComponent

/**
 * Base class for table and column selection dialogs.
 */
abstract class StatementCreationOptionsDialog<T : TableContextView>(
  project: Project,
  actionTitle: String,
  protected val view: T,
  private val context: StatementGenerationContext
) :
  DialogWrapper(project, false)
{

  /**
   * The currently selected table from the list of tables.
   */
  protected var selectedCreateStatement: SqlCreateTableStmt

  init {
    val activeIndex = context.activeIndex
    selectedCreateStatement = context.createTableStatements[activeIndex]

    title = actionTitle
    myHelpAction.isEnabled = false
    myOKAction.isEnabled = true
    myCancelAction.isEnabled = true
  }

  final override fun createCenterPanel(): JComponent {
    setupMenuPanel()
    initializeUIState()
    return view.rootPanel
  }

  /**
   * Called to initialize the UI as needed.
   */
  protected abstract fun initializeUIState()

  /**
   * Called when the table selected in the table menu changes.
   */
  protected abstract fun onTableSelectionChanged(createTableStatement: SqlCreateTableStmt)

  private fun setupMenuPanel() {
    val createTableStatements = context.createTableStatements

    if (createTableStatements.size == 1) {
      view.menuPanel.isVisible = false
    } else {
      val activeIndex = context.activeIndex

      view.tableMenuModel.addAll(createTableStatements.map(::CreateTableStatementItem))
      view.tableMenu.selectedIndex = activeIndex
      view.tableMenu.addActionListener { event ->
        val cb = event.source as JComboBox<*>
        val createTableStatement = (cb.selectedItem as CreateTableStatementItem).createTableStmt

        selectedCreateStatement = createTableStatement
        onTableSelectionChanged(createTableStatement)
      }

      view.menuPanel.isVisible = true
    }
  }

}
