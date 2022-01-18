package app.cash.sqldelight.intellij.run

import com.intellij.openapi.project.Project

internal class FakeDialogViewFactory(
  val values: List<String> = emptyList(),
  val okPressed: Boolean = true,
) : ArgumentsInputDialog.Factory {

  override fun create(project: Project, parameters: List<SqlParameter>): ArgumentsInputDialog {
    return object : ArgumentsInputDialog {
      override val result: List<SqlParameter>
        get() = parameters.zip(values) { a, b ->
          a.copy(value = b)
        }

      override fun showAndGet(): Boolean {
        return okPressed
      }
    }
  }
}