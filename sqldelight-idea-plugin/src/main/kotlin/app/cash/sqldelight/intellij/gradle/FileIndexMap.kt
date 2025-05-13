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
import com.intellij.util.lang.ClassPath
import com.intellij.util.lang.UrlClassLoader
import io.ktor.util.rootCause
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.ServiceLoader
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import timber.log.Timber

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
          if (!module.isDisposed && !module.project.isDisposed) {
            try {
              ProgressManager.getInstance().runProcessWithProgressAsynchronously(
                FetchModuleModels(module, projectPath),
                EmptyProgressIndicator().apply { start() },
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
    }
    return fileIndices[projectPath] ?: defaultIndex
  }

  private inner class FetchModuleModels(
    private val module: Module,
    private val projectPath: String,
  ) : Task.Backgroundable(
    /* project = */
    module.project,
    /* title = */
    "Importing ${module.name} SQLDelight",
  ) {
    override fun run(indicator: ProgressIndicator) {
      FileIndexingNotification.getInstance(module.project).unconfiguredReason = Syncing

      val executionSettings = GradleExecutionSettings(
        /* gradleHome = */
        null,
        /* serviceDirectory = */
        null,
        /* distributionType = */
        DistributionType.DEFAULT_WRAPPED,
        /* isOfflineWork = */
        false,
      )
      try {
        fileIndices.putAll(
          GradleExecutionHelper().execute(projectPath, executionSettings) { connection ->
            fetchThread = Thread.currentThread()
            if (!initialized) return@execute emptyMap()

            Timber.i("Fetching SQLDelight models")
            val javaHome = (
              ExternalSystemJdkUtil.getAvailableJdk(project).second.homePath
                ?: ExternalSystemJdkUtil.getJavaHome()
              )?.let { File(it) }

            Timber.i("Using java home $javaHome")

            val properties =
              connection.action(FetchProjectModelsBuildAction).setJavaHome(javaHome).run()

            Timber.i("Assembling file index")
            return@execute properties.mapValues { (_, value) ->
              val compatibility = if (value == null) {
                Incompatible("The IDE and Gradle versions of SQLDelight are incompatible, please update the lower version.")
              } else {
                GradleCompatibility.validate(value)
              }

              if (compatibility is Incompatible) {
                FileIndexingNotification.getInstance(project).unconfiguredReason =
                  FileIndexingNotification.UnconfiguredReason.Incompatible(compatibility.reason, null)
                return@mapValues defaultIndex
              }

              val pluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId("com.squareup.sqldelight"))!!
              val shouldInvalidate = pluginDescriptor.addDialect(
                value!!.dialectJars.map { it.toURI() },
              )

              val database = value.databases.first()
              SqlDelightProjectService.getInstance(module.project).apply {
                val dialect = ServiceLoader.load(SqlDelightDialect::class.java, pluginDescriptor.pluginClassLoader).first()
                setDialect(dialect, shouldInvalidate)
                treatNullAsUnknownForEquality = database.treatNullAsUnknownForEquality
                generateAsync = database.generateAsync
              }

              return@mapValues FileIndex(database)
            }
          },
        )
        Timber.i("Initialized file index")
        EditorNotifications.getInstance(module.project).updateAllNotifications()
      } catch (externalException: ExternalSystemException) {
        // Expected interrupt from calling close() on the index.
        if (externalException.rootCause() !is InterruptedException) {
          // It's a gradle error, ignore and let the user fix when they try and build the project
          Timber.i("sqldelight model gen failed")
          Timber.i(externalException)

          FileIndexingNotification.getInstance(project).unconfiguredReason =
            FileIndexingNotification.UnconfiguredReason.Incompatible(
              """
              Connecting with the SQLDelight plugin failed: try building from the command line.
              """.trimIndent(),
              externalException,
            )
        }
      } finally {
        fetchThread = null
        initialized = false
      }
    }
  }

  private fun Throwable.rootCause(): Throwable = cause?.rootCause() ?: this

  companion object {
    internal var defaultIndex: SqlDelightFileIndex = SqlDelightFileIndexImpl()

    private var previouslyAddedDialect: Collection<Path>? = null

    @Suppress("UnstableApiUsage", "UNCHECKED_CAST") // Naughty method.
    private fun PluginDescriptor.addDialect(uris: Collection<URI>): Boolean {
      val dialectPath = uris.map(Path::of)
      val shouldInvalidate = previouslyAddedDialect != dialectPath
      val pluginClassLoader = pluginClassLoader as UrlClassLoader

      // We need to remove the last loaded dialect as well as add our new one.
      val files = try {
        UrlClassLoader::class.java.getDeclaredField("files").let { field ->
          field.isAccessible = true
          val result = field.get(pluginClassLoader) as List<Path>
          field.isAccessible = false
          return@let result
        }
      } catch (e: NoSuchFieldException) {
        // This is a newer version of IntelliJ that doesn't have the files field on UrlClassLoader,
        // reflect on Classpath instead.
        ClassPath::class.java.getDeclaredField("files").let { field ->
          field.isAccessible = true
          val result = (field.get(pluginClassLoader.classPath) as Array<Path>).toList()
          field.isAccessible = false
          return@let result
        }
      }

      // Filter out the last loaded dialect.
      val filtered = files.filter { it != previouslyAddedDialect }
      val newClasspath = filtered + dialectPath
      previouslyAddedDialect = dialectPath

      // Add the new one in.
      try {
        // older IntelliJ versions have a reset method that takes a list of files.
        ClassPath::class.java.getDeclaredMethod("reset", List::class.java).let { method ->
          method.isAccessible = true
          method.invoke(pluginClassLoader.classPath, newClasspath)
          method.isAccessible = false
        }
      } catch (e: NoSuchMethodException) {
        // in newer versions of IntelliJ, call both argless reset and set files reflectively.
        ClassPath::class.java.getDeclaredMethod("reset").let { method ->
          method.isAccessible = true
          method.invoke(pluginClassLoader.classPath)
          method.isAccessible = false
        }
        ClassPath::class.java.getDeclaredField("files").let { field ->
          field.isAccessible = true
          field.set(pluginClassLoader.classPath, newClasspath.toTypedArray())
          field.isAccessible = false
        }
      }

      return shouldInvalidate
    }
  }
}
