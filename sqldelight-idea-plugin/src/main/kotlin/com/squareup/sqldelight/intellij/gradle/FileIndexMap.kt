package com.squareup.sqldelight.intellij.gradle

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightProjectService
import com.squareup.sqldelight.core.dialectPreset
import com.squareup.sqldelight.intellij.FileIndex
import com.squareup.sqldelight.intellij.SqlDelightFileIndexImpl
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import timber.log.Timber

internal class FileIndexMap {
  private val fileIndices = mutableMapOf<String, SqlDelightFileIndex>()

  private var initializing = false

  operator fun get(module: Module): SqlDelightFileIndex {
    val projectPath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return defaultIndex
    val result = fileIndices[projectPath]
    if (result != null) return result
    synchronized(this) {
      if (!initializing) {
        initializing = true
        ProgressManager.getInstance().run(FetchModuleModels(module, projectPath))
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
      fileIndices.putAll(GradleExecutionHelper().execute(projectPath, executionSettings) { connection ->
        Timber.i("Fetching SQLDelight models")
        val properties = connection.action(FetchProjectModelsBuildAction).run()

        Timber.i("Assembling file index")
        return@execute properties.mapValues { (_, value) ->
          if (value == null) return@mapValues defaultIndex

          val database = value.databases.first()
          SqlDelightProjectService.getInstance(module.project).dialectPreset = database.dialectPreset
          return@mapValues FileIndex(database)
        }
      })

      Timber.i("Initialized file index")
      initializing = false
    }
  }

  companion object {
    internal var defaultIndex: SqlDelightFileIndex = SqlDelightFileIndexImpl()
  }
}
