package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification.UnconfiguredReason.GradleSyncing
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType.RESOLVE_PROJECT
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID

class GradleSystemListener : ExternalSystemTaskNotificationListenerAdapter() {
  override fun onStart(
    id: ExternalSystemTaskId,
    workingDir: String?
  ) {
    if (id.projectSystemId == GRADLE_SYSTEM_ID && id.type == RESOLVE_PROJECT) {
      // Gradle sync just started, pause our existing import if one is happening.
      id.findProject()?.let { project ->
        SqlDelightProjectService.getInstance(project).clearIndex()
        FileIndexingNotification.getInstance(project).unconfiguredReason = GradleSyncing
      }
    }
  }

  override fun onSuccess(id: ExternalSystemTaskId) {
    if (id.projectSystemId == GRADLE_SYSTEM_ID && id.type == RESOLVE_PROJECT) {
      // Gradle sync just finished, reset the file index.
      id.findProject()?.let { project ->
        SqlDelightProjectService.getInstance(project).resetIndex()
      }
    }
  }
}
