package app.cash.sqldelight.multiplatform

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.konan.target.HostManager

class MultiplatformConventions : Plugin<Project> {
  override fun apply(project: Project) {
    project.plugins.apply("org.jetbrains.kotlin.multiplatform")

    (project.kotlinExtension as KotlinMultiplatformExtension).apply {
      val nativeMain = sourceSets.create("nativeMain").apply {
        dependsOn(sourceSets.getByName("commonMain"))
      }
      val nativeTest = sourceSets.create("nativeTest").apply {
        dependsOn(sourceSets.getByName("commonTest"))
      }

      targets.whenObjectAdded {
        if (it is KotlinNativeTarget) {
          sourceSets.getByName("${it.name}Main").dependsOn(nativeMain)
          sourceSets.getByName("${it.name}Test").dependsOn(nativeTest)
        }

        if (it is KotlinJsTarget) {
          it.browser {
            testTask {
              useKarma {
                useChromeHeadless()
              }
            }
          }
          it.compilations.all {
            it.kotlinOptions.apply {
              moduleKind = "umd"
              sourceMap = true
              sourceMapEmbedSources = null
            }
          }
        }
      }

      jvm()

      js {
        browser()
      }

      iosX64()
      iosArm64()
      tvosX64()
      tvosArm64()
      watchosX64()
      watchosArm32()
      watchosArm64()
      macosX64()
      mingwX64()
      linuxX64()
      macosArm64()
      iosSimulatorArm64()
      watchosSimulatorArm64()
      tvosSimulatorArm64()

      // linking fails for the linux test build if not built on a linux host
      // ensure the tests and linking for them is only done on linux hosts
      project.tasks.named("linuxX64Test") { it.enabled = HostManager.hostIsLinux }
      project.tasks.named("linkDebugTestLinuxX64") { it.enabled = HostManager.hostIsLinux }

      project.tasks.named("mingwX64Test") { it.enabled = HostManager.hostIsMingw }
      project.tasks.named("linkDebugTestMingwX64") { it.enabled = HostManager.hostIsMingw }
    }
  }
}
