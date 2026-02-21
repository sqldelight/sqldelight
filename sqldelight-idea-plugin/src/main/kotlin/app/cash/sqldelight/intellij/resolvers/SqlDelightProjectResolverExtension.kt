package app.cash.sqldelight.intellij.resolvers

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightPropertiesFile
import app.cash.sqldelight.core.SqlDelightSourceFolder
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ModuleData
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

val SQL_DELIGHT_MODEL_KEY = Key.create(SqlDelightPropertiesFile::class.java, 1)

class SqlDelightProjectResolverExtension : AbstractProjectResolverExtension() {

  // Grabs the model and sets the data during sync
  override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
    val sqlDelightModel = resolverCtx.getExtraProject(gradleModule, SqlDelightPropertiesFile::class.java)
    if (sqlDelightModel != null) { // Copy the proxy object, as it cannot be serialized, to an internal model
      val propertiesFileModel = SqlDelightPropertiesFileModel(
        sqlDelightModel.databases.map { databaseProperties ->
          SqlDelightDatabasePropertiesModel(
            databaseProperties.packageName,
            databaseProperties.compilationUnits.map {
              SqlDelightCompilationUnitModel(
                it.name,
                it.sourceFolders.map { sourceFolder -> SqlDelightSourceFolderModel(sourceFolder.folder) }.toSet(),
                it.outputDirectoryFile,
              )
            },
            databaseProperties.className,
            databaseProperties.dependencies.map { databaseName ->
              SqlDelightDatabaseNameModel(databaseName.packageName, databaseName.className)
            },
            databaseProperties.deriveSchemaFromMigrations,
            databaseProperties.treatNullAsUnknownForEquality,
            databaseProperties.generateAsync,
            databaseProperties.rootDirectory,
          )
        },
        sqlDelightModel.dialectJars.toMutableList(),
        sqlDelightModel.minimumSupportedVersion,
        sqlDelightModel.currentVersion,
      )

      ideModule.createChild(SQL_DELIGHT_MODEL_KEY, propertiesFileModel)
    }
    super.populateModuleExtraModels(gradleModule, ideModule)
  }

  // Tells the IDE which classes are available for the Gradle Tooling API to query
  override fun getExtraProjectModelClasses(): Set<Class<*>?> {
    return setOf(SqlDelightPropertiesFile::class.java)
  }
}

data class SqlDelightPropertiesFileModel(
  override val databases: List<SqlDelightDatabaseProperties>,
  override val dialectJars: Collection<File>,
  override val minimumSupportedVersion: String,
  override val currentVersion: String,
) : SqlDelightPropertiesFile

data class SqlDelightDatabasePropertiesModel(
  @Input override val packageName: String,
  @Nested override val compilationUnits: List<SqlDelightCompilationUnit>,
  @Input override val className: String,
  @Nested override val dependencies: List<SqlDelightDatabaseName>,
  @Input override val deriveSchemaFromMigrations: Boolean = false,
  @Input override val treatNullAsUnknownForEquality: Boolean = false,
  @Input override val generateAsync: Boolean = false,
  @Internal override val rootDirectory: File,
) : SqlDelightDatabaseProperties

data class SqlDelightDatabaseNameModel(
  @Input override val packageName: String,
  @Input override val className: String,
) : SqlDelightDatabaseName

data class SqlDelightCompilationUnitModel(
  @Input override val name: String,
  @Nested override val sourceFolders: Set<SqlDelightSourceFolder>,
  // Output directory is already cached [SqlDelightTask.outputDirectory].
  @Internal override val outputDirectoryFile: File,
) : SqlDelightCompilationUnit

data class SqlDelightSourceFolderModel(
  // Sources are already cached [SqlDelightTask.getSources]
  @Internal override val folder: File,
  @Input override val dependency: Boolean = false,
) : SqlDelightSourceFolder
