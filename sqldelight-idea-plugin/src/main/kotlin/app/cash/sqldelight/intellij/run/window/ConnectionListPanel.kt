package app.cash.sqldelight.intellij.run.window

import app.cash.sqldelight.dialect.api.ConnectionManager
import app.cash.sqldelight.intellij.run.ConnectionOptions
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.AnActionButton
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel

internal class ConnectionListPanel(
  private val connectionOptions: ConnectionOptions,
  private val connectionManager: ConnectionManager,
  private val project: Project,
) : JPanel() {
  private val list = JBList<String>().apply {
    selectionMode = ListSelectionModel.SINGLE_SELECTION
    layoutOrientation = JList.VERTICAL
    setEmptyText("Connections")
  }

  private val clearSelectedConnection: AnActionButton = object : AnActionButton(
    "Stop", "Stop using connection", AllIcons.RunConfigurations.ShowIgnored
  ) {
    init {
      isEnabled = false
    }

    override fun actionPerformed(e: AnActionEvent) {
      connectionOptions.unselectOption()
      updateModel()
    }
  }

  init {
    layout = GridBagLayout()
    add(
      ToolbarDecorator.createDecorator(list)
        .setAddAction {
          val properties = connectionManager.createNewConnectionProperties(project)
            ?: return@setAddAction
          connectionOptions.addOption(properties)
          updateModel()
        }
        .setRemoveAction {
          connectionOptions.removeOption(list.selectedValue)
          updateModel()
        }
        .setMoveUpAction { moveSelected(list.selectedIndex - 1) }
        .setMoveDownAction { moveSelected(list.selectedIndex + 1) }
        .addExtraAction(clearSelectedConnection)
        .createPanel(),
      GridBagConstraints().apply {
        fill = GridBagConstraints.BOTH
        weighty = 1.0
        weightx = 1.0
        gridx = 0
        gridy = 0
      }
    )

    list.addListSelectionListener {
      if (list.selectedValue == null) {
        clearSelectedConnection.isEnabled = false
        connectionOptions.unselectOption()
      } else {
        clearSelectedConnection.isEnabled = true
        connectionOptions.selectOption(list.selectedValue)
      }
      updateModel()
      PsiDocumentManager.getInstance(project).reparseFiles(emptyList(), true)
    }

    list.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.clickCount == 2) {
          val currentProperties = connectionOptions.currentOption()
          val properties = connectionManager.createNewConnectionProperties(project, currentProperties)
            ?: return
          connectionOptions.replaceOption(properties)
          updateModel()
        }
      }
    })

    updateModel()
  }

  private fun moveSelected(to: Int) {
    val selectedIndex = list.selectedIndex
    val newOrder = connectionOptions.getKeys().toMutableList().apply {
      val removed = removeAt(selectedIndex)
      add(to, removed)
    }
    connectionOptions.reorderKeys(newOrder)
    updateModel()
    list.selectedIndex = to
  }

  private fun JComponent.connectionPanel(): ConnectionListPanel? {
    return components.firstNotNullResult {
      when (it) {
        is ConnectionListPanel -> it
        is JComponent -> it.connectionPanel()
        else -> null
      }
    }
  }

  private fun updateModel(includeOtherContent: Boolean = true) {
    // If our keys have updated, refresh the model.
    val keys = connectionOptions.getKeys()
    if ((list.model as? CollectionListModel)?.toList() != keys) {
      list.model = CollectionListModel(keys)
    }

    // If the selection has changed, refresh the item selected.
    val newSelection = connectionOptions.selectedOption()?.let { selectedOption ->
      connectionOptions.getKeys().indexOf(selectedOption)
    }
    if (list.selectedIndex != newSelection) {
      if (newSelection == null) list.clearSelection()
      else list.selectedIndex = newSelection
    }

    // Other content panes should reflect the changes made in this one.
    if (includeOtherContent) {
      ToolWindowManager.getInstance(project).getToolWindow("SqlDelight")!!.contentManager
        .contents.forEach {
          it.component.connectionPanel()?.updateModel(false)
        }
    }
  }
}
