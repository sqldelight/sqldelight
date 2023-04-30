package app.cash.sqldelight.intellij.actions.generatemenu.ui.views

import app.cash.sqldelight.intellij.actions.generatemenu.ui.CreateTableStatementItem
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import java.awt.Dimension
import javax.swing.*

/**
 * A base view that provides a [JPanel] with a table selection [ComboBox]. Subclasses are responsible
 * placing the [menuPanel] into the [rootPanel].
 */
open class TableContextView {

  val rootPanel = JPanel()

  val menuPanel = JPanel()
  val tableMenuModel = DefaultComboBoxModel<CreateTableStatementItem>()
  val tableMenu = ComboBox(tableMenuModel)

  val menuLabel = JBLabel("Select table:")

  init {
    menuPanel.layout = BoxLayout(menuPanel, BoxLayout.LINE_AXIS)

    tableMenu.renderer = DefaultListCellRenderer()

    menuPanel.add(menuLabel)
    menuPanel.add(Box.createRigidArea(Dimension(5, 0)))
    menuPanel.add(tableMenu)
  }

}