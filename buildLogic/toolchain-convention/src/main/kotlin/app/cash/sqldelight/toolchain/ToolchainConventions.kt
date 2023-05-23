package app.cash.sqldelight.toolchain

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

abstract class ToolchainConventions(private val jdkVersion: Int) : Plugin<Project> {
    override fun apply(project: Project) {
        project.kotlinExtension.jvmToolchain { spec ->
            spec.languageVersion.set(JavaLanguageVersion.of(jdkVersion))
            spec.vendor.set(JvmVendorSpec.AZUL)
        }
    }
}

// Controls the minimum JDK version required to use the SQLDelight runtime
class RuntimeToolchainConventions : ToolchainConventions(8)

// Controls the minimum JDK version required to run the SQLDelight plugin and compiler
class CompilerToolchainConventions : ToolchainConventions(11)
