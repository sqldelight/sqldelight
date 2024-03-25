package app.cash.sqldelight.multiplatform

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.konan.target.HostManager

class MultiplatformConventions : Plugin<Project> {
  override fun apply(project: Project) {
    project.plugins.apply("org.jetbrains.kotlin.multiplatform")

    (project.kotlinExtension as KotlinMultiplatformExtension).apply {
      compilerOptions {
        this.freeCompilerArgs.addAll(
          "-Xexpect-actual-classes",
        )
      }

      jvm()

      @OptIn(ExperimentalWasmDsl::class)
      listOf(js(), wasmJs()).forEach {
        it.browser {
          testTask {
            it.useKarma {
              useChromeHeadless()
            }
          }
        }
        compilerOptions {
          moduleKind.set(JsModuleKind.MODULE_UMD)
        }
      }

      @OptIn(ExperimentalKotlinGradlePluginApi::class)
      applyDefaultHierarchyTemplate {
        common {
          group("jsCommon") {
            withJs()
            withWasm()
          }
        }
      }

      // tier 1
      linuxX64()
      macosX64()
      macosArm64()
      iosSimulatorArm64()
      iosX64()

      // tier 2
      linuxArm64()
      watchosSimulatorArm64()
      watchosX64()
      watchosArm32()
      watchosArm64()
      tvosSimulatorArm64()
      tvosX64()
      tvosArm64()
      iosArm64()

      // tier 3
      androidNativeArm32()
      androidNativeArm64()
      androidNativeX86()
      androidNativeX64()
      mingwX64()
      watchosDeviceArm64()

      // linking fails for the linux test build if not built on a linux host
      // ensure the tests and linking for them is only done on linux hosts
      project.tasks.named("linuxX64Test") { it.enabled = HostManager.hostIsLinux }
      project.tasks.named("linkDebugTestLinuxX64") { it.enabled = HostManager.hostIsLinux }

      project.tasks.named("mingwX64Test") { it.enabled = HostManager.hostIsMingw }
      project.tasks.named("linkDebugTestMingwX64") { it.enabled = HostManager.hostIsMingw }
    }
  }
}
