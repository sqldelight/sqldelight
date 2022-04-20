package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.SqlDelightProjectService
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.propComponentProperty
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileTypeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.RecentsManager
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.buttonGroup
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.selected
import javax.swing.JComponent
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal const val RECENT_DB_PATH = "app.cash.sqldelight.recentPath"
internal const val DB_CONNECTION_TYPE = "app.cash.sqldelight.connectionType"

internal class SelectConnectionTypeAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    if (project != null) {
      SelectConnectionTypeDialog(project).show()
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = SqlDelightProjectService.getInstance(project).dialect.isSqlite
  }
}

internal enum class ConnectionType(val type: Int) {
  NONE(0),
  FILE(1)
}

private class ConnectionTypeProperty(project: Project) : ReadWriteProperty<Any, ConnectionType> {
  private val propertiesComponent = PropertiesComponent.getInstance(project)

  override fun getValue(thisRef: Any, property: KProperty<*>): ConnectionType {
    val type = propertiesComponent.getInt(DB_CONNECTION_TYPE, ConnectionType.NONE.type)
    return ConnectionType.values().firstOrNull { it.type == type } ?: ConnectionType.NONE
  }

  override fun setValue(thisRef: Any, property: KProperty<*>, value: ConnectionType) {
    propertiesComponent.setValue(
      DB_CONNECTION_TYPE,
      value.type,
      ConnectionType.NONE.type
    )
  }
}

private fun connectionType(project: Project): ReadWriteProperty<Any, ConnectionType> =
  ConnectionTypeProperty(project)

internal class ConnectionOptions(val project: Project) {
  var filePath: String by propComponentProperty(project, "")
  var connectionType: ConnectionType by connectionType(project)
}

internal class SelectConnectionTypeDialog(
  private val project: Project,
  private val options: ConnectionOptions = ConnectionOptions(project),
) : DialogWrapper(project) {

  private val recentsManager: RecentsManager = RecentsManager.getInstance(project)

  init {
    title = "Select Connection Type"
    init()
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      buttonGroup(options::connectionType) {
        row {
          val r = radioButton("File", ConnectionType.FILE)
          row {
            textFieldWithHistoryWithBrowseButton(
              browseDialogTitle = "Choose File",
              getter = { options.filePath },
              setter = { options.filePath = it },
              fileChooserDescriptor = FileTypeDescriptor("Choose File", "db"),
              historyProvider = { recentsManager.getRecentEntries(RECENT_DB_PATH).orEmpty() },
              fileChosen = { vFile ->
                vFile.path.also { path ->
                  options.filePath = path
                  recentsManager.registerRecentEntry(RECENT_DB_PATH, path)
                }
              }
            )
              .withValidationOnInput(validateFilePath(r.selected))
              .withValidationOnApply(validateFilePath(r.selected))
          }
        }
        row {
          radioButton("None", ConnectionType.NONE)
        }
      }
    }
  }

  private fun validateFilePath(selected: () -> Boolean): ValidationInfoBuilder.(TextFieldWithHistoryWithBrowseButton) -> ValidationInfo? = {
    if (selected() && it.text.isEmpty()) {
      error("The file path is empty")
    } else {
      null
    }
  }
}
