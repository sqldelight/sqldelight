package com.squareup.sqldelight.intellij.gradle

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.squareup.sqldelight.core.GradleCompatibility
import com.squareup.sqldelight.core.GradleCompatibility.CompatibilityReport.Incompatible
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightProjectService
import com.squareup.sqldelight.core.dialectPreset
import com.squareup.sqldelight.intellij.FileIndex
import com.squareup.sqldelight.intellij.SqlDelightFileIndexImpl
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import timber.log.Timber
import java.io.File

internal class FileIndexMap {
  private val fileIndices = mutableMapOf<String, SqlDelightFileIndex>()
  private val incompatibilityNotifier = IncompatibilityNotifier()

  private var initialized = false
  private var retries = 0

  operator fun get(module: Module): SqlDelightFileIndex {
    val projectPath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return defaultIndex
    val result = fileIndices[projectPath]
    if (result != null) return result
    synchronized(this) {
      if (!initialized) {
        initialized = true
        if (!module.isDisposed && !module.project.isDisposed)
          try {
            ProgressManager.getInstance().run(FetchModuleModels(module, projectPath))
          } catch (e: Throwable) {
            // IntelliJ can fail to start the fetch command, reinitialize later in this case.
            if (retries++ < 3) {
              initialized = false
            }
          }
      }
    }
    return fileIndices[projectPath] ?: defaultIndex
  }

  private inner class FetchModuleModels(
    private val module: Module,
    private val projectPath: String
  ) : Task.Backgroundable(
    /* project = */ module.project,
    /* title = */ "Importing ${module.name} SQLDelight"
  ) {
    override fun run(indicator: ProgressIndicator) {
      val executionSettings = GradleExecutionSettings(
        /* gradleHome = */ null,
        /* serviceDirectory = */ null,
        /* distributionType = */ DistributionType.DEFAULT_WRAPPED,
        /* isOfflineWork = */ false
      )
      try {
        fileIndices.putAll(
          GradleExecutionHelper().execute(projectPath, executionSettings) { connection ->
            Timber.i("Fetching SQLDelight models")
            val javaHome =
              ExternalSystemJdkUtil.getJdk(project, ExternalSystemJdkUtil.USE_PROJECT_JDK)
                ?.homeDirectory?.path?.let { File(it) }
            val properties =
              connection.action(FetchProjectModelsBuildAction).setJavaHome(javaHome).run()

            Timber.i("Assembling file index")
            return@execute properties.mapValues { (_, value) ->
              if (value == null) return@mapValues defaultIndex

              val compatibility = GradleCompatibility.validate(value)
              if (compatibility is Incompatible) {
                incompatibilityNotifier.notifyError(project, compatibility.reason)
                return@mapValues defaultIndex
              }

              val database = value.databases.first()
              SqlDelightProjectService.getInstance(module.project).dialectPreset =
                database.dialectPreset
              return@mapValues FileIndex(database)
            }
          }
        )
      } catch (externalException: ExternalSystemException) {
        // It's a gradle error, ignore and let the user fix when they try and build the project
      }

      Timber.i("Initialized file index")
      initialized = false
    }
  }

  companion object {
    internal var defaultIndex: SqlDelightFileIndex = SqlDelightFileIndexImpl()
  }
}

private class IncompatibilityNotifier {
  private var showedIncompatibilityBalloon = false

  fun notifyError(project: Project, content: String) {
    if (showedIncompatibilityBalloon) return

    // We only want to show this balloon once per project open.
    showedIncompatibilityBalloon = true
    NOTIFICATION_GROUP.createNotification(content, NotificationType.ERROR).notify(project)
  }

  companion object {
    private val NOTIFICATION_GROUP =
      NotificationGroup("SQLDelight Notification Group", NotificationDisplayType.BALLOON, true)
  }
}
