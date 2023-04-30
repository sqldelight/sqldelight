package app.cash.sqldelight.intellij.actions.generatemenu.ui.views

import com.intellij.ui.border.CustomLineBorder
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class TableSelectionWithSingleColumnListView : TableContextView() {

  val columnList: JBList<String>
  val columnListModel = DefaultListModel<String>()

  init {
    rootPanel.layout = BorderLayout(5, 5)
    rootPanel.add(menuPanel, BorderLayout.PAGE_START)

    columnList = JBList(columnListModel)

    val scroll = JScrollPane(columnList)
    scroll.border = CustomLineBorder(1, 1, 1, 1)
    rootPanel.add(scroll, BorderLayout.CENTER)

    rootPanel.minimumSize = Dimension(400, 200)
  }

}
