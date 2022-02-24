package app.cash.sqldelight.intellij.notifications

import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.core.lang.MigrationFileType
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification.UnconfiguredReason.GradleSyncing
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification.UnconfiguredReason.Incompatible
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification.UnconfiguredReason.Syncing
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import java.awt.Color

class FileIndexingNotification() : EditorNotifications.Provider<EditorNotificationPanel>(), DumbAware {
  private val KEY = Key.create<EditorNotificationPanel>("app.cash.sqldelight.indexing")

  override fun getKey() = KEY

  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
    if (file.fileType != SqlDelightFileType && file.fileType != MigrationFileType) return null

    val service = SqlDelightProjectService.getInstance(project)
    val module = service.module(file) ?: return null

    if (!service.fileIndex(module).isConfigured) {
      val message = when (val unconfiguredReason = unconfiguredReason) {
        is GradleSyncing -> "SQLDelight is waiting for Gradle to finish syncing."
        is Syncing -> "SQLDelight is setting up..."
        is Incompatible -> unconfiguredReason.errorMessage
      }
      return EditorNotificationPanel(Color.decode("#7e56c2")).apply {
        text = message
      }
    }

    return null
  }

  companion object {
    internal var unconfiguredReason: UnconfiguredReason = GradleSyncing
  }

  internal sealed interface UnconfiguredReason {
    object GradleSyncing : UnconfiguredReason
    data class Incompatible(val errorMessage: String) : UnconfiguredReason
    object Syncing : UnconfiguredReason
  }
}