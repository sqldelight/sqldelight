package app.cash.sqldelight.intellij.run.window

import app.cash.sqldelight.dialect.api.ConnectionManager
import app.cash.sqldelight.intellij.run.ConnectionOptions
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel

internal class ConnectionListPanel(
  private val connectionOptions: ConnectionOptions,
  private val connectionManager: ConnectionManager,
  private val project: Project,
) : JPanel() {
  private val list = JBList(*connectionOptions.getKeys().toTypedArray())

  init {
    layout = GridBagLayout()
    add(JBScrollPane(list.apply {
      selectionMode = ListSelectionModel.SINGLE_SELECTION
      layoutOrientation = JList.VERTICAL
      addListSelectionListener {
        connectionOptions.selectedOption = list.selectedValue ?: ""
      }
    }), GridBagConstraints().apply {
      fill = GridBagConstraints.BOTH
      weighty = 1.0
      weightx = 1.0
      gridx = 0
      gridy = 0
    })
    add(JButton("Add Connection").apply {
      addActionListener {
        val properties = connectionManager.createNewConnectionProperties(project)
          ?: return@addActionListener
        connectionOptions.addOption(properties)
        list.model = JBList.createDefaultListModel(*connectionOptions.getKeys().toTypedArray())
      }
    }, GridBagConstraints().apply {
      fill = GridBagConstraints.HORIZONTAL
      weighty = 0.0
      weightx = 1.0
      gridx = 0
      gridy = 1
    })
  }
}