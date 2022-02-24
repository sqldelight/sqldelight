package app.cash.sqldelight.intellij.gradle

import app.cash.sqldelight.core.GradleCompatibility
import app.cash.sqldelight.core.GradleCompatibility.CompatibilityReport.Incompatible
import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.core.dialectPreset
import app.cash.sqldelight.intellij.FileIndex
import app.cash.sqldelight.intellij.SqlDelightFileIndexImpl
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification.UnconfiguredReason.Syncing
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.ui.EditorNotifications
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import timber.log.Timber
import java.io.File

internal class FileIndexMap {
  private val fileIndices = mutableMapOf<String, SqlDelightFileIndex>()

  private var initialized = false
  private var retries = 0

  operator fun get(module: Module): SqlDelightFileIndex {
    val projectPath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return defaultIndex
    val result = fileIndices[projectPath]
    if (result != null) return result
    ApplicationManager.getApplication().invokeLater {
      synchronized(this) {
        if (!initialized) {
          initialized = true
          if (!module.isDisposed && !module.project.isDisposed)
            try {
              val progressIndicator = EmptyProgressIndicator()
              ProgressManager.getInstance().runProcessWithProgressAsynchronously(
                  FetchModuleModels(module, projectPath),
                  progressIndicator
              )
              progressIndicator.start()

              FileIndexingNotification.unconfiguredReason = Syncing
              EditorNotifications.getInstance(module.project).updateAllNotifications()
            } catch (e: Throwable) {
              // IntelliJ can fail to start the fetch command, reinitialize later in this case.
              if (retries++ < 3) {
                initialized = false
              }
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
                FileIndexingNotification.unconfiguredReason = FileIndexingNotification.UnconfiguredReason.Incompatible(compatibility.reason)
                EditorNotifications.getInstance(project).updateAllNotifications()
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
