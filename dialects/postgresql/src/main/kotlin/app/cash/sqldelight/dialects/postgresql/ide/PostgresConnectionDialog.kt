package app.cash.sqldelight.dialects.postgresql.ide

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ValidationInfoBuilder
import javax.swing.JComponent
import javax.swing.JTextField

internal class PostgresConnectionDialog(
  project: Project,
  private var connectionName: String? = null,
  internal var host: String = "localhost",
  internal var port: String = "5432",
  internal var databaseName: String = "",
  internal var username: String = "",
  internal var password: String = "",
) : DialogWrapper(project) {
  internal val connectionKey get() = connectionName ?: "Default Postgres"

  init {
    title = "Create PostgreSQL Connection"
    init()
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row("Connection Name") {
        textField().bindText({ connectionKey }, { connectionName = it }).validationOnApply(validateNonEmpty(CONNECTION_NAME_NON_EMPTY))
          .columns(COLUMNS_MEDIUM)
          .applyToComponent {
            if (connectionName != null) this.isEditable = false
          }
      }
      row("Host") {
        textField().bindText({ host }, { host = it }).validationOnApply(validateNonEmpty(HOST_NON_EMPTY))
          .columns(COLUMNS_MEDIUM)
      }
      row("Port") {
        textField().bindText({ port }, { port = it }).validationOnApply(validateNonEmpty(PORT_NON_EMPTY))
          .columns(COLUMNS_MEDIUM)
      }
      row("Database Name") {
        textField().bindText({ databaseName }, { databaseName = it })
          .columns(COLUMNS_MEDIUM)
      }
      row("Username") {
        textField().bindText({ username }, { username = it })
          .columns(COLUMNS_MEDIUM)
      }
      row("Password") {
        textField().bindText({ password }, { password = it })
          .columns(COLUMNS_MEDIUM)
      }
    }
  }

  companion object {
    private const val CONNECTION_NAME_NON_EMPTY = "You must supply a connection name."
    private const val HOST_NON_EMPTY = "You must supply a host."
    private const val PORT_NON_EMPTY = "You must supply a port."
  }
}

private fun validateNonEmpty(message: String): ValidationInfoBuilder.(JTextField) -> ValidationInfo? = {
  if (it.text.isNullOrEmpty()) error(message) else null
}
