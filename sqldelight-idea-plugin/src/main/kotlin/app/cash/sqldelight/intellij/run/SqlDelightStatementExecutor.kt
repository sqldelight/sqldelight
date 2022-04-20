package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.ConnectionManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.execution.ui.actions.CloseAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import com.jakewharton.picnic.BorderStyle
import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.renderText
import com.jakewharton.picnic.table
import org.jetbrains.annotations.TestOnly
import java.awt.BorderLayout
import java.sql.ResultSet
import java.sql.SQLException
import javax.swing.JComponent
import javax.swing.JPanel

internal interface SqlDelightStatementExecutor {
  fun execute(sqlStmt: String)

  companion object {
    fun getInstance(project: Project): SqlDelightStatementExecutor {
      return ServiceManager.getService(project, SqlDelightStatementExecutor::class.java)!!
    }
  }
}

internal class SqlDelightStatementExecutorImpl @NonInjectable @TestOnly constructor(
  private val project: Project,
  private val connectionManager: ConnectionManager
) : SqlDelightStatementExecutor {

  @Suppress("unused")
  constructor(project: Project) : this(
    project = project,
    connectionManager = ConnectionManager.getInstance(project)
  )

  override fun execute(sqlStmt: String) {
    val consoleView = getConsoleView(project) ?: return
    try {
      val connectionOptions = ConnectionOptions(project)
      val path = connectionOptions.filePath
      if (path.isEmpty()) return

      connectionManager.getConnection(path).use { connection ->
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

  private fun getConsoleView(project: Project): ConsoleView? {
    val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
    val toolbarActions = DefaultActionGroup()
    val panel: JComponent = JPanel(BorderLayout())
    panel.add(consoleView.component, BorderLayout.CENTER)
    val toolbar = ActionManager.getInstance().createActionToolbar("RunIdeConsole", toolbarActions, false)
    toolbar.setTargetComponent(consoleView.component)
    panel.add(toolbar.component, BorderLayout.WEST)
    val descriptor = RunContentDescriptor(consoleView, null, panel, "SqlDelight")
    val executor = DefaultRunExecutor.getRunExecutorInstance()
    toolbarActions.addAll(*consoleView.createConsoleActions())
    toolbarActions.add(CloseAction(executor, descriptor, project))
    RunContentManager.getInstance(project).showRunContent(executor, descriptor)
    return descriptor.executionConsole as? ConsoleView
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
