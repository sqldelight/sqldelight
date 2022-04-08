package app.cash.sqldelight.intellij.gradle

import app.cash.sqldelight.core.GradleCompatibility
import app.cash.sqldelight.core.GradleCompatibility.CompatibilityReport.Incompatible
import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.intellij.FileIndex
import app.cash.sqldelight.intellij.SqlDelightFileIndexImpl
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification.UnconfiguredReason.Syncing
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.ui.EditorNotifications
import com.intellij.util.lang.UrlClassLoader
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import timber.log.Timber
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.ServiceLoader

internal class FileIndexMap {
  private var fetchThread: Thread? = null
  private val fileIndices = mutableMapOf<String, SqlDelightFileIndex>()

  private var initialized = false
  private var retries = 0

  fun close() {
    fetchThread?.interrupt()
    fetchThread = null
    initialized = false
  }

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
              ProgressManager.getInstance().runProcessWithProgressAsynchronously(
                FetchModuleModels(module, projectPath),
                EmptyProgressIndicator().apply { start() }
              )
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
      FileIndexingNotification.getInstance(module.project).unconfiguredReason = Syncing

      val executionSettings = GradleExecutionSettings(
        /* gradleHome = */ null,
        /* serviceDirectory = */ null,
        /* distributionType = */ DistributionType.DEFAULT_WRAPPED,
        /* isOfflineWork = */ false
      )
      try {
        fileIndices.putAll(
          GradleExecutionHelper().execute(projectPath, executionSettings) { connection ->
            fetchThread = Thread.currentThread()
            if (!initialized) return@execute emptyMap()

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
                FileIndexingNotification.getInstance(project).unconfiguredReason =
                  FileIndexingNotification.UnconfiguredReason.Incompatible(compatibility.reason)
                return@mapValues defaultIndex
              }

              val pluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId("com.squareup.sqldelight"))!!
              val shouldInvalidate = pluginDescriptor.addDialect(
                listOf(value.dialectJar.toURI()) +
                  value.moduleJars.map { it.toURI() }
              )

              val database = value.databases.first()
              SqlDelightProjectService.getInstance(module.project).apply {
                val dialect = ServiceLoader.load(SqlDelightDialect::class.java, pluginDescriptor.pluginClassLoader).single()
                setDialect(dialect, shouldInvalidate)
                treatNullAsUnknownForEquality = database.treatNullAsUnknownForEquality
              }

              return@mapValues FileIndex(database)
            }
          }
        )
        Timber.i("Initialized file index")
        EditorNotifications.getInstance(module.project).updateAllNotifications()
      } catch (externalException: ExternalSystemException) {
        // It's a gradle error, ignore and let the user fix when they try and build the project
      } finally {
        fetchThread = null
        initialized = false
      }
    }
  }

  companion object {
    internal var defaultIndex: SqlDelightFileIndex = SqlDelightFileIndexImpl()

    private var previouslyAddedDialect: Collection<Path>? = null

    @Suppress("UnstableApiUsage", "UNCHECKED_CAST") // Naughty method.
    private fun PluginDescriptor.addDialect(uris: Collection<URI>): Boolean {
      val dialectPath = uris.map(Path::of)
      val shouldInvalidate = previouslyAddedDialect != dialectPath
      val pluginClassLoader = pluginClassLoader as UrlClassLoader

      // We need to remove the last loaded dialect as well as add our new one.
      val files = UrlClassLoader::class.java.getDeclaredField("files").let { field ->
        field.isAccessible = true
        val result = field.get(pluginClassLoader) as MutableList<Path>
        field.isAccessible = false
        return@let result
      }

      // Remove the last loaded dialect.
      previouslyAddedDialect?.let {
        files.removeAll(it)
      }
      previouslyAddedDialect = dialectPath

      // Add the new one in.
      files.addAll(dialectPath)
      pluginClassLoader.classPath.reset(files)

      return shouldInvalidate
    }
  }
}
