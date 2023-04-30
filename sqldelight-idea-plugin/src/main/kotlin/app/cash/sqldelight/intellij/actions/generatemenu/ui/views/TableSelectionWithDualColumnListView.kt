package app.cash.sqldelight.intellij.actions.generatemenu.ui.views

import com.intellij.ui.border.CustomLineBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class TableSelectionWithDualColumnListView : TableContextView() {

  val leftColumnList: JBList<String>
  val rightColumnList: JBList<String>

  val leftColumnListModel = DefaultListModel<String>()
  val rightColumnListModel = DefaultListModel<String>()

  val leftColumnLabel = JBLabel("Returned columns:")
  val rightColumnLabel = JBLabel("Where columns:")

  init {
    leftColumnList = JBList(leftColumnListModel)
    val leftColumnListScrollPane = JScrollPane(leftColumnList)
    leftColumnListScrollPane.border = CustomLineBorder(1, 1, 1, 1)

    rightColumnList = JBList(rightColumnListModel)
    val rightColumnScrollPane = JScrollPane(rightColumnList)
    rightColumnScrollPane.border = CustomLineBorder(1, 1, 1, 1)

    val layout = BorderLayout(5, 5)
    rootPanel.layout = layout
    rootPanel.add(menuPanel, BorderLayout.PAGE_START)

    val columnsSelectionPanel = JPanel()
    columnsSelectionPanel.layout = GridBagLayout()

    val c = GridBagConstraints()
    c.gridx = 0
    c.gridy = 0
    c.anchor = GridBagConstraints.LINE_START
    c.insets = JBUI.insetsBottom(10)
    columnsSelectionPanel.add(leftColumnLabel, c)

    c.gridx = 1
    c.insets = JBUI.insets(0, 7, 10, 0)
    columnsSelectionPanel.add(rightColumnLabel, c)

    c.gridx = 0
    c.gridy = 1
    c.insets = JBUI.insetsRight(7)
    c.weightx = 1.0
    c.weighty = 1.0
    c.fill = GridBagConstraints.BOTH
    columnsSelectionPanel.add(leftColumnListScrollPane, c)

    c.gridx = 1
    c.insets = JBUI.insetsLeft(7)
    c.fill = GridBagConstraints.BOTH
    columnsSelectionPanel.add(rightColumnScrollPane, c)

    rootPanel.add(columnsSelectionPanel, BorderLayout.CENTER)

    rootPanel.minimumSize = Dimension(400, 200)
  }

}

//private fun createWithBoxLayout(): JPanel {
//  val panel = JPanel()
//  val layout = BoxLayout(panel, BoxLayout.PAGE_AXIS)
//  panel.layout = layout
//
//
//  val createTableStatements = modelData.createTableStatements
//  val defaultListModel = DefaultListModel<String>()
//
//  val activeIndex = modelData.activeIndex
//
//  defaultListModel.addAll(createTableStatements[activeIndex].columnDefList.map { it.columnName.name })
//
//  if (createTableStatements.size > 1) {
//    val menuPanel = JPanel()
//    menuPanel.layout = BoxLayout(menuPanel, BoxLayout.LINE_AXIS)
//
//    val tableNames = createTableStatements.map(::CreateTableStatementItem).toTypedArray()
//    val tableMenu = ComboBox(tableNames)
//    tableMenu.selectedIndex = activeIndex
//
//    val menuLabel = JBLabel("Select table:")
//    tableMenu.renderer = DefaultListCellRenderer()
//
//    tableMenu.addActionListener { event ->
//      val cb = event.source as JComboBox<*>
//      val createTableStatement = (cb.selectedItem as CreateTableStatementItem).createTableStmt
//      //.map { it.tableName.name }
//
//      defaultListModel.removeAllElements()
//      defaultListModel.addAll(createTableStatement.columnDefList.map { it.columnName.name })
//    }
//    menuPanel.add(menuLabel)
//    menuPanel.add(Box.createRigidArea(Dimension(5, 0)))
//    menuPanel.add(tableMenu)
//    panel.add(menuPanel, BorderLayout.PAGE_START)
//  }
//
//  val columnsSelectionPanel = JPanel()
//  columnsSelectionPanel.layout = GridBagLayout()
//
//  val list = JBList(defaultListModel)
//  list.maximumSize = null
//  list.border = CustomLineBorder(1, 1, 1, 1)
//  //      gridLayoutManager.addLayoutComponent(list, )
//
//  val c = GridBagConstraints()
//  c.gridx = 0
//  c.gridy = 0
//  panel.add(list, c)
//
//  panel.add(list, c)
//
//  return panel
//}
//
//private fun createWithSpringLayout(): JPanel {
//  centerPanel = JPanel()
//  //      val gridLayoutManager = GridLayoutManager(2, 1)
//  //      panel.layout = gridLayoutManager
//
//  val layout = SpringLayout()
//  centerPanel.layout = layout
//
//  val defaultListModel = DefaultListModel<String>()
//
//  val list = JBList(defaultListModel)
//  list.border = CustomLineBorder(1, 1, 1, 1)
//  //      gridLayoutManager.addLayoutComponent(list, )
//
//  centerPanel.add(list)
//  val createTableStatements = modelData.createTableStatements
//
//  val activeIndex = modelData.activeIndex
//
//  defaultListModel.addAll(createTableStatements[activeIndex].columnDefList.map { it.columnName.name })
//
//  if (createTableStatements.size == 1) {
//    layout.putConstraint(
//      SpringLayout.WEST, list,
//      5,
//      SpringLayout.WEST, centerPanel
//    )
//    layout.putConstraint(
//      SpringLayout.NORTH, list,
//      5,
//      SpringLayout.NORTH, centerPanel
//    )
//  } else {
//    val tableNames = createTableStatements.map(::CreateTableStatementItem).toTypedArray()
//    val tableMenu = ComboBox(tableNames)
//    tableMenu.selectedIndex = activeIndex
//
//    val menuLabel = JBLabel("Select table:")
//    tableMenu.renderer = DefaultListCellRenderer()
//
//    tableMenu.addActionListener { event ->
//      val cb = event.source as JComboBox<*>
//      val createTableStatement = (cb.selectedItem as CreateTableStatementItem).createTableStmt
//      //.map { it.tableName.name }
//
//      defaultListModel.removeAllElements()
//      defaultListModel.addAll(createTableStatement.columnDefList.map { it.columnName.name })
//    }
//    centerPanel.add(menuLabel)
//    centerPanel.add(tableMenu)
//    layout.putConstraint(
//      SpringLayout.WEST, menuLabel,
//      5,
//      SpringLayout.WEST, centerPanel
//    )
//    layout.putConstraint(
//      SpringLayout.VERTICAL_CENTER, menuLabel,
//      0,
//      SpringLayout.VERTICAL_CENTER, tableMenu
//    )
//
//    layout.putConstraint(
//      SpringLayout.WEST, tableMenu,
//      5,
//      SpringLayout.EAST, menuLabel
//    )
//    layout.putConstraint(
//      SpringLayout.NORTH, tableMenu,
//      5,
//      SpringLayout.NORTH, contentPane
//    )
//
//    layout.putConstraint(
//      SpringLayout.WEST, list,
//      5,
//      SpringLayout.WEST, centerPanel
//    )
//    layout.putConstraint(
//      SpringLayout.NORTH, list,
//      5,
//      SpringLayout.SOUTH, tableMenu
//    )
//    layout.putConstraint(
//      SpringLayout.EAST, list,
//      -5,
//      SpringLayout.EAST, centerPanel
//    )
//    layout.putConstraint(
//      SpringLayout.SOUTH, list,
//      -5,
//      SpringLayout.SOUTH, centerPanel
//    )
//
//  }
//
//  return centerPanel
//}

