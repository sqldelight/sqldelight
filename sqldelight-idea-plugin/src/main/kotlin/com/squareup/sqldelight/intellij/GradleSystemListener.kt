package com.squareup.sqldelight.intellij

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.squareup.sqldelight.core.SqlDelightProjectService
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID

class GradleSystemListener : ExternalSystemTaskNotificationListenerAdapter() {
  override fun onSuccess(id: ExternalSystemTaskId) {
    if (id.projectSystemId == GRADLE_SYSTEM_ID) {
      // Gradle sync just finished, reset the file index.
      id.findProject()?.let { project ->
        SqlDelightProjectService.getInstance(project).resetIndex()
      }
    }
  }
}
