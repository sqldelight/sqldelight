package app.cash.sqldelight.intellij.run.window

import app.cash.sqldelight.dialect.api.ConnectionManager
import app.cash.sqldelight.intellij.run.ConnectionOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Insets
import javax.swing.GroupLayout
import javax.swing.JPanel

internal class SqlDelightToolWindowFactory(
  private val connectionManager: ConnectionManager
) : ToolWindowFactory {
  override fun createToolWindowContent(
    project: Project,
    toolWindow: ToolWindow
  ) {
    val runSqlText = JPanel(BorderLayout()).apply {
      add(
        JBTextArea("Create a connection to get started.").apply {
          margin = Insets(8, 8, 8, 8)
          isEditable = false
        },
        BorderLayout.CENTER
      )
    }

    toolWindow.contentManager.apply {
      val content = createWithConnectionSidePanel(project, connectionManager, runSqlText)
      content.isCloseable = false
      addContent(content)
    }
  }
}

internal fun ContentManager.createWithConnectionSidePanel(
  project: Project,
  connectionManager: ConnectionManager,
  component: Component,
  name: String? = null
): Content {
  val panel = JPanel()

  val layout = GroupLayout(panel)
  panel.layout = layout

  val connectionList = ConnectionListPanel(ConnectionOptions(project), connectionManager, project)

  layout.setHorizontalGroup(
    layout.createSequentialGroup()
      .addComponent(connectionList, 50, 200, 300)
      .addComponent(component)
  )

  layout.setVerticalGroup(
    layout.createParallelGroup()
      .addComponent(connectionList)
      .addComponent(component)
  )

  return factory.createContent(panel, name, false)
}
