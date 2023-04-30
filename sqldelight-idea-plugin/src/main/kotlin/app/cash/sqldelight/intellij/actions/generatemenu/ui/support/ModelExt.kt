package app.cash.sqldelight.intellij.actions.generatemenu.ui.support

import javax.swing.DefaultListModel

/**
 * Removes all elements from the model and replaces them the passed items.
 */
fun <T : Any> DefaultListModel<T>.replaceAll(elements: Collection<T>) {
  removeAllElements()
  addAll(elements)
}