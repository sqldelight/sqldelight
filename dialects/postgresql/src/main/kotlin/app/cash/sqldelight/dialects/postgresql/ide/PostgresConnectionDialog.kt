package app.cash.sqldelight.dialects.postgresql.ide

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.panel
import javax.swing.JComponent
import javax.swing.JTextField

internal class PostgresConnectionDialog(project: Project) : DialogWrapper(project) {

  init {
    title = "Create PostgreSQL Connection"
    init()
  }

  internal var connectionName: String = "Default Postgres"
  internal var host: String = "localhost"
  internal var port: String = "5432"
  internal var databaseName: String = ""
  internal var username: String = ""
  internal var password: String = ""

  override fun createCenterPanel(): JComponent {
    return panel {
      row("Connection Name") {
        textField(
          getter = { connectionName },
          setter = { connectionName = it }
        ).withValidationOnApply(validateNonEmpty(connectionNameNonEmpty))
          .withValidationOnInput(validateNonEmpty(connectionNameNonEmpty))
      }
      row("Host") {
        textField(
          getter = { host },
          setter = { host = it }
        ).withValidationOnApply(validateNonEmpty(hostNonEmpty))
          .withValidationOnInput(validateNonEmpty(hostNonEmpty))
      }
      row("Connection Name") {
        textField(
          getter = { port },
          setter = { port = it }
        ).withValidationOnApply(validateNonEmpty(portNonEmpty))
          .withValidationOnInput(validateNonEmpty(portNonEmpty))
      }
      row("Database Name") {
        textField(
          getter = { databaseName },
          setter = { databaseName = it }
        )
      }
      row("Username") {
        textField(
          getter = { username },
          setter = { username = it }
        )
      }
      row("Password") {
        textField(
          getter = { password },
          setter = { password = it }
        )
      }
    }
  }

  companion object {
    private const val connectionNameNonEmpty = "You must supply a connection name."
    private const val hostNonEmpty = "You must supply a host."
    private const val portNonEmpty = "You must supply a port."
  }
}

private fun validateNonEmpty(message: String): ValidationInfoBuilder.(JTextField) -> ValidationInfo? =
  {
    if (it.text.isNullOrEmpty()) error(message) else null
  }
