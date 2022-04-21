package app.cash.sqldelight.dialects.sqlite_3_18

import app.cash.sqldelight.dialect.api.ConnectionManager.ConnectionProperties
import com.intellij.openapi.fileChooser.FileTypeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.RecentsManager
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.panel
import java.io.File
import javax.swing.JComponent
import javax.swing.JTextField

private const val RECENT_DB_PATH = "app.cash.sqldelight.recentPath"

internal class SelectConnectionTypeDialog(
  project: Project,
) : DialogWrapper(project) {
  private val recentsManager: RecentsManager = RecentsManager.getInstance(project)

  private var connectionName: String = ""
  private var filePath: String = ""

  init {
    title = "Create SQLite Connection"
    init()
  }

  fun connectionProperties(): ConnectionProperties {
    return ConnectionProperties(connectionName, filePath)
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row("Connection Name") {
        textField(
          getter = { connectionName },
          setter = { connectionName = it }
        ).withValidationOnApply(validateKey())
          .withValidationOnInput(validateKey())
      }
      row(label = "DB File Path") {
        textFieldWithHistoryWithBrowseButton(
          browseDialogTitle = "Choose File",
          getter = { filePath },
          setter = { filePath = it },
          fileChooserDescriptor = FileTypeDescriptor("Choose File", "db"),
          historyProvider = { recentsManager.getRecentEntries(RECENT_DB_PATH).orEmpty() },
          fileChosen = { vFile ->
            vFile.path.also { path ->
              filePath = path
              recentsManager.registerRecentEntry(RECENT_DB_PATH, path)
            }
          }
        )
          .withValidationOnInput(validateFilePath())
          .withValidationOnApply(validateFilePath())
      }
    }.also {
      validate()
    }
  }
}

private fun validateKey(): ValidationInfoBuilder.(JTextField) -> ValidationInfo? =
  {
    if (it.text.isNullOrEmpty()) {
      error("You must supply a connection key.")
    } else {
      null
    }
  }

private fun validateFilePath(): ValidationInfoBuilder.(TextFieldWithHistoryWithBrowseButton) -> ValidationInfo? =
  {
    if (it.text.isEmpty()) {
      error("The file path is empty.")
    } else if (!File(it.text).exists()) {
      error("This file does not exist.")
    } else {
      null
    }
  }
