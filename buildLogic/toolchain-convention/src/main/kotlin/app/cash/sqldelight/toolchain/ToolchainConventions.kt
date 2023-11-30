package app.cash.sqldelight.toolchain

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

abstract class ToolchainConventions(private val targetJdkVersion: String) : Plugin<Project> {
  override fun apply(project: Project) {
    project.kotlinExtension.jvmToolchain { spec ->
      spec.languageVersion.set(JavaLanguageVersion.of(BUILD_JDK))
      spec.vendor.set(JvmVendorSpec.AZUL)
    }

    project.tasks.withType(KotlinCompile::class.java).configureEach { task ->
      task.compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(targetJdkVersion))
      }
    }

    project.extensions.findByType(JavaPluginExtension::class.java)?.apply {
      sourceCompatibility = JavaVersion.toVersion(targetJdkVersion)
      targetCompatibility = JavaVersion.toVersion(targetJdkVersion)
    }

    project.extensions.findByType(CommonExtension::class.java)?.compileOptions?.apply {
      sourceCompatibility = JavaVersion.toVersion(targetJdkVersion)
      targetCompatibility = JavaVersion.toVersion(targetJdkVersion)
    }
  }

  companion object {
    const val BUILD_JDK = 17
  }
}

// Controls the minimum JDK version required to use the SQLDelight runtime
class RuntimeToolchainConventions : ToolchainConventions("1.8")

// Controls the minimum JDK version required to run the SQLDelight plugin and compiler
class CompilerToolchainConventions : ToolchainConventions("17")
