package app.cash.sqldelight.toolchain

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

abstract class ToolchainConventions(private val targetJdkVersion: String) : Plugin<Project> {
  override fun apply(project: Project) {
    if (JavaVersion.current() != BUILD_JAVA_VERSION) {
      project.kotlinExtension.jvmToolchain { spec ->
        spec.languageVersion.set(
          JavaLanguageVersion.of(BUILD_JDK),
        )
        spec.vendor.set(JvmVendorSpec.AZUL)
      }
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
    private val HIGHEST_SUPPORTED_JAVA_VERSION: JavaVersion = when {
      GradleVersion.current() < GradleVersion.version("8.3") -> JavaVersion.VERSION_17
      else -> JavaVersion.VERSION_20
    }

    private val LOWEST_SUPPORTED_JAVA_VERSION: JavaVersion = JavaVersion.VERSION_17

    private val BUILD_JAVA_VERSION: JavaVersion = when {
      JavaVersion.current() < LOWEST_SUPPORTED_JAVA_VERSION -> LOWEST_SUPPORTED_JAVA_VERSION
      JavaVersion.current() > HIGHEST_SUPPORTED_JAVA_VERSION -> HIGHEST_SUPPORTED_JAVA_VERSION
      else -> JavaVersion.current()
    }

    val BUILD_JDK = BUILD_JAVA_VERSION.majorVersion.toInt()
  }
}

// Controls the minimum JDK version required to use the SQLDelight runtime
class RuntimeToolchainConventions : ToolchainConventions("1.8")

// Controls the minimum JDK version required to run the SQLDelight plugin and compiler
class CompilerToolchainConventions : ToolchainConventions("17")
