package com.squareup.sqldelight.intellij.gradle

import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.UnknownModelException

object FetchProjectModelsBuildAction : BuildAction<Map<String, SqlDelightPropertiesFile?>> {
  override fun execute(controller: BuildController): Map<String, SqlDelightPropertiesFile?> {
    val models = mutableMapOf<String, SqlDelightPropertiesFile?>()
    for (project in controller.buildModel.projects) {
      try {
        models[project.projectDirectory.absolutePath] = controller.getModel(project, SqlDelightPropertiesFile::class.java)
      } catch (e: UnknownModelException) {
        models[project.projectDirectory.absolutePath] = null
      }
    }
    return models
  }
}
