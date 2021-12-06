package app.cash.sqldelight.intellij.gradle

import app.cash.sqldelight.core.SqlDelightPropertiesFile
import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.UnknownModelException

object FetchProjectModelsBuildAction : BuildAction<Map<String, SqlDelightPropertiesFile?>> {
  override fun execute(controller: BuildController): Map<String, SqlDelightPropertiesFile?> {
    val models = mutableMapOf<String, SqlDelightPropertiesFile?>()
    for (project in controller.buildModel.projects) {
      // Make sure the keys of our map always use '/' as path separator
      val path = project.projectDirectory.absoluteFile.invariantSeparatorsPath
      try {
        models[path] = controller.getModel(project, SqlDelightPropertiesFile::class.java)
      } catch (e: UnknownModelException) {
        models[path] = null
      }
    }
    return models
  }
}
