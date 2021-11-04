package com.squareup.sqldelight

import org.gradle.testkit.runner.GradleRunner
import java.io.File

internal fun GradleRunner.withCommonConfiguration(projectRoot: File): GradleRunner {
  val gradleRoot = File(projectRoot, "gradle").apply {
    mkdir()
  }
  File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)
  File(projectRoot, "gradle.properties").writeText(
    """
      |org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
      |android.useAndroidX=true
      |
    """.trimMargin()
  )
  File(projectRoot, "local.properties").apply {
    if (!exists()) writeText("sdk.dir=${androidHome()}\n")
  }
  return withProjectDir(projectRoot)
}
