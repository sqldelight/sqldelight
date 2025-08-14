package app.cash.sqldelight.intellij.gradle

import app.cash.sqldelight.core.GradleCompatibility
import app.cash.sqldelight.core.GradleCompatibility.CompatibilityReport.Incompatible
import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.intellij.FileIndex
import app.cash.sqldelight.intellij.SqlDelightFileIndexImpl
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification
import app.cash.sqldelight.intellij.resolvers.SQL_DELIGHT_MODEL_KEY
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.util.lang.ClassPath
import com.intellij.util.lang.UrlClassLoader
import java.net.URI
import java.nio.file.Path
import java.util.ServiceLoader
import org.jetbrains.kotlin.idea.base.externalSystem.findAll
import org.jetbrains.kotlin.idea.configuration.GRADLE_SYSTEM_ID

internal class FileIndexMap {
  private val fileIndices = mutableMapOf<String, SqlDelightFileIndex>()

  operator fun get(module: Module): SqlDelightFileIndex {
    val projectPath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return defaultIndex

    // Get the DataNode for this module
    val moduleDataNode = ExternalSystemApiUtil.findModuleNode(module.project, GRADLE_SYSTEM_ID, projectPath) ?: return defaultIndex

    // Get the DataNode containing the propertiesFile object
    val propertiesFileNode = moduleDataNode.findAll(SQL_DELIGHT_MODEL_KEY).singleOrNull() ?: return defaultIndex

    // Get the actual propertiesFile object
    val propertiesFile = propertiesFileNode.data

    return fileIndices.computeIfAbsent(projectPath) {
      val compatibility = GradleCompatibility.validate(propertiesFile)

      if (compatibility is Incompatible) {
        FileIndexingNotification.getInstance(module.project).unconfiguredReason =
          FileIndexingNotification.UnconfiguredReason.Incompatible(compatibility.reason, null)
        return@computeIfAbsent defaultIndex
      }

      val pluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId("com.squareup.sqldelight"))!!
      val shouldInvalidate = pluginDescriptor.addDialect(
        propertiesFile.dialectJars.map { it.toURI() },
      )

      val database = propertiesFile.databases.first()
      SqlDelightProjectService.getInstance(module.project).apply {
        val dialect = ServiceLoader.load(SqlDelightDialect::class.java, pluginDescriptor.pluginClassLoader).first()
        setDialect(dialect, shouldInvalidate)
        treatNullAsUnknownForEquality = database.treatNullAsUnknownForEquality
        generateAsync = database.generateAsync
      }

      return@computeIfAbsent FileIndex(database)
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
