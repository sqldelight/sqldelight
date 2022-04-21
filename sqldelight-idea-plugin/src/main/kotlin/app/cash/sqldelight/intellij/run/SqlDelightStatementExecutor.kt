package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.lang.psi.StmtIdentifier
import app.cash.sqldelight.dialect.api.ConnectionManager
import app.cash.sqldelight.intellij.run.window.createWithConnectionSidePanel
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.jakewharton.picnic.BorderStyle
import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.renderText
import com.jakewharton.picnic.table
import java.awt.BorderLayout
import java.sql.ResultSet
import java.sql.SQLException
import javax.swing.JComponent
import javax.swing.JPanel

internal class SqlDelightStatementExecutor(
  private val project: Project,
  private val connectionManager: ConnectionManager
) {
  fun execute(sqlStmt: String, identifier: StmtIdentifier?) {
    val consoleView = getConsoleView(project, identifier) ?: return
    try {
      val connectionOptions = ConnectionOptions(project)

      connectionManager.getConnection(connectionOptions.selectedProperties()).use { connection ->
        val statement = connection.createStatement()
        val hasResult = statement.execute(sqlStmt)
        if (hasResult) {
          processResultSet(consoleView, statement.resultSet)
        } else {
          val text = "Query executed successfully (affected ${statement.updateCount} rows)"
          consoleView.print("$text\n", ConsoleViewContentType.NORMAL_OUTPUT)
        }
      }
    } catch (e: SQLException) {
      val message = e.message ?: "Unknown error during execution of query $sqlStmt"
      consoleView.print("$message\n", ConsoleViewContentType.LOG_ERROR_OUTPUT)
    }
  }

  private fun getConsoleView(project: Project, identifier: StmtIdentifier?): ConsoleView? {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("SqlDelight") ?: return null
    val contentManager = toolWindow.contentManager

    // Create the console view.
    val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
    val toolbarActions = DefaultActionGroup()
    val panel: JComponent = JPanel(BorderLayout())
    panel.add(consoleView.component, BorderLayout.CENTER)
    val toolbar = ActionManager.getInstance().createActionToolbar("RunIdeConsole", toolbarActions, false)
    toolbar.setTargetComponent(consoleView.component)
    panel.add(toolbar.component, BorderLayout.WEST)

    // Create a new content tab inside the tool window.
    val newExecution = contentManager.createWithConnectionSidePanel(
      project,
      connectionManager,
      consoleView.component,
      name = identifier?.name ?: "SQL"
    )
    contentManager.addContent(newExecution)

    // Request that the content is focused.
    contentManager.setSelectedContent(newExecution, true)
    toolWindow.activate(null)

    return consoleView
  }

  private fun processResultSet(consoleView: ConsoleView, resultSet: ResultSet) {
    val metaData = resultSet.metaData
    val range = 1..metaData.columnCount
    val columnNames = range.map(metaData::getColumnName)
    val rows = mutableListOf<List<String>>()
    while (resultSet.next()) {
      rows += range.map(resultSet::getString)
    }
    val table = table {
      style {
        borderStyle = BorderStyle.Solid
      }
      cellStyle {
        alignment = TextAlignment.MiddleCenter
        border = true
      }
      row(*columnNames.toTypedArray())
      rows.forEach { row(*it.toTypedArray()) }
    }
    consoleView.print("${table.renderText()}\n", ConsoleViewContentType.NORMAL_OUTPUT)
  }
}
