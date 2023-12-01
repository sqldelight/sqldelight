package app.cash.sqldelight.intellij.settings

import com.intellij.codeInspection.javaDoc.JavadocUIUtil.bindCheckbox
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.xmlb.XmlSerializerUtil
import javax.swing.JComponent
import org.jetbrains.annotations.Nls

@State(
  name = "app.cash.sqldelight.settings.AppSettingsState",
  storages = [Storage("SqldelightPlugin.xml")],
)
@Service(Service.Level.PROJECT)
class SettingsState : PersistentStateComponent<SettingsState> {
  var showSystemTablesInAutoCompletion = false

  override fun getState(): SettingsState = this

  override fun loadState(state: SettingsState) {
    XmlSerializerUtil.copyBean(state, this)
  }
}

private class UIState(
  var showSystemTablesInAutoCompletion: Boolean = false,
)

class AppSettings(project: Project) : Configurable {
  private val persistentState = project.service<SettingsState>()
  private val uiState: UIState

  init {
    uiState = UIState(
      showSystemTablesInAutoCompletion = persistentState.showSystemTablesInAutoCompletion,
    )
  }

  override fun createComponent(): JComponent = panel {
    row {
      checkBox("Show system tables in auto-completion")
        .bindCheckbox(uiState::showSystemTablesInAutoCompletion)
    }
  }

  override fun isModified(): Boolean {
    return uiState.showSystemTablesInAutoCompletion != persistentState.showSystemTablesInAutoCompletion
  }

  override fun apply() {
    persistentState.showSystemTablesInAutoCompletion = uiState.showSystemTablesInAutoCompletion
  }

  override fun reset() {
    uiState.showSystemTablesInAutoCompletion = persistentState.showSystemTablesInAutoCompletion
  }

  @Nls(capitalization = Nls.Capitalization.Title)
  override fun getDisplayName(): String = "SqlDelight"
}
