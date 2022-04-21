package app.cash.sqldelight.intellij.run

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import javax.swing.JComponent

internal interface ArgumentsInputDialog {
  val result: List<SqlParameter>

  fun showAndGet(): Boolean

  interface Factory {
    fun create(project: Project, parameters: List<SqlParameter>): ArgumentsInputDialog
  }
}

internal class ArgumentsInputDialogImpl(
  project: Project,
  private val parameters: List<SqlParameter>
) : DialogWrapper(project), ArgumentsInputDialog {

  init {
    init()
  }

  private val _result = mutableListOf<SqlParameter>()
  override val result: List<SqlParameter> get() = _result

  override fun createCenterPanel(): JComponent {
    return panel {
      parameters.forEach { parameter ->
        row("${parameter.name}:") {
          textField(parameter::value, {
            _result.add(parameter.copy(value = it))
          })
        }
      }
    }
  }
}

internal class ArgumentsInputDialogFactoryImpl : ArgumentsInputDialog.Factory {
  override fun create(project: Project, parameters: List<SqlParameter>): ArgumentsInputDialog {
    return ArgumentsInputDialogImpl(project, parameters)
  }
}
