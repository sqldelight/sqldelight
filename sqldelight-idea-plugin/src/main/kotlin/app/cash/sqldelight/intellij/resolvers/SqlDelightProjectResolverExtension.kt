package app.cash.sqldelight.intellij.resolvers

import app.cash.sqldelight.core.SqlDelightPropertiesFile
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

val SQL_DELIGHT_MODEL_KEY = Key.create(SqlDelightPropertiesFile::class.java, 1)

class SqlDelightProjectResolverExtension : AbstractProjectResolverExtension() {

  // Grabs the model and sets the data during sync
  override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
    val sqlDelightModel = resolverCtx.getExtraProject(gradleModule, SqlDelightPropertiesFile::class.java)
    if (sqlDelightModel != null) {
      ideModule.createChild(SQL_DELIGHT_MODEL_KEY, sqlDelightModel)
    }
    super.populateModuleExtraModels(gradleModule, ideModule)
  }

  // Tells the IDE which classes are available for the Gradle Tooling API to query
  override fun getExtraProjectModelClasses(): Set<Class<*>?> {
    return setOf(SqlDelightPropertiesFile::class.java)
  }
}
