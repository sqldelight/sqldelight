package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification.UnconfiguredReason.GradleSyncing
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType.RESOLVE_PROJECT
import org.jetbrains.kotlin.idea.configuration.GRADLE_SYSTEM_ID

class GradleSystemListener : ExternalSystemTaskNotificationListener {
  override fun onStart(
    projectPath: String,
    id: ExternalSystemTaskId,
  ) {
    if (id.projectSystemId == GRADLE_SYSTEM_ID && id.type == RESOLVE_PROJECT) {
      // Gradle sync just started, pause our existing import if one is happening.
      id.findProject()?.let { project ->
        SqlDelightProjectService.getInstance(project).clearIndex()
        FileIndexingNotification.getInstance(project).unconfiguredReason = GradleSyncing
      }
    }
  }

  override fun onSuccess(projectPath: String, id: ExternalSystemTaskId) {
    if (id.projectSystemId == GRADLE_SYSTEM_ID && id.type == RESOLVE_PROJECT) {
      // Gradle sync just finished, reset the file index.
      id.findProject()?.let { project ->
        SqlDelightProjectService.getInstance(project).resetIndex()
      }
    }
  }
}
