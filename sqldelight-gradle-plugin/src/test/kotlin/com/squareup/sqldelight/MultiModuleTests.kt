package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.SqlDelightCompilationUnit
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import java.io.File

class MultiModuleTests {
  @Test
  fun `sqldelight dependencies are added to the compilation unit`() {
    val androidHome = androidHome()
    val fixtureRoot = File("src/test/multi-module")
    File(fixtureRoot, ".idea").mkdir()
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
        .withArguments("clean", "--stacktrace")
        .build()

    // verify
    val propertiesFile = File(fixtureRoot, ".idea/sqldelight/ProjectA/${SqlDelightPropertiesFile.NAME}")
    assertThat(propertiesFile.exists()).isTrue()

    val properties = SqlDelightPropertiesFile.fromFile(propertiesFile).databases.single()
    assertThat(properties.packageName).isEqualTo("com.example")
    assertThat(properties.outputDirectory).isEqualTo("build/sqldelight/Database")
    assertThat(properties.compilationUnits).hasSize(1)

    with(properties.compilationUnits[0]) {
      assertThat(sourceFolders).containsExactly("src/main/sqldelight", "../ProjectB/src/main/sqldelight")
    }

    propertiesFile.delete()
  }

  @Test
  fun integrationTests() {
    val androidHome = androidHome()
    val integrationRoot = File("src/test/multi-module")
    File(integrationRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    val gradleRoot = File(integrationRoot, "gradle").apply {
      mkdir()
    }
    File("../gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)

    val runner = GradleRunner.create()
        .withProjectDir(integrationRoot)
        .withPluginClasspath()
        .withArguments("clean", ":ProjectA:check", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
  @Test
  fun `the android target of a multiplatform module is a dependency for an android only module`() {
    val androidHome = androidHome()
    val fixtureRoot = File("src/test/multi-module")
    File(fixtureRoot, ".idea").mkdir()
    File(fixtureRoot, "local.properties").writeText("sdk.dir=$androidHome\n")

    GradleRunner.create()
        .withProjectDir(fixtureRoot)
        .withPluginClasspath()
        .withArguments("clean", "--stacktrace")
        .build()

    // verify
    val propertiesFile = File(fixtureRoot, ".idea/sqldelight/AndroidProject/${SqlDelightPropertiesFile.NAME}")
    assertThat(propertiesFile.exists()).isTrue()

    val properties = SqlDelightPropertiesFile.fromFile(propertiesFile).databases.single()
    assertThat(properties.packageName).isEqualTo("com.sample")
    assertThat(properties.outputDirectory).isEqualTo("build/sqldelight/CommonDb")
    assertThat(properties.compilationUnits).containsExactly(
        SqlDelightCompilationUnit(
            name = "minApi23DemoDebug",
            sourceFolders = listOf(
                "src/main/sqldelight",
                "src/demo/sqldelight",
                "src/minApi23/sqldelight",
                "src/minApi23Demo/sqldelight",
                "src/debug/sqldelight",
                "src/minApi23DemoDebug/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23DemoDebug/sqldelight",
                "../MultiplatformProject/src/androidLibMain/sqldelight",
                "../MultiplatformProject/src/androidLibDemo/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23Demo/sqldelight",
                "../MultiplatformProject/src/androidLibDebug/sqldelight",
                "../MultiplatformProject/src/commonMain/sqldelight")),
        SqlDelightCompilationUnit(
            name = "minApi23DemoRelease",
            sourceFolders = listOf(
                "src/main/sqldelight",
                "src/demo/sqldelight",
                "src/minApi23/sqldelight",
                "src/minApi23Demo/sqldelight",
                "src/release/sqldelight",
                "src/minApi23DemoRelease/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23DemoRelease/sqldelight",
                "../MultiplatformProject/src/androidLibMain/sqldelight",
                "../MultiplatformProject/src/androidLibDemo/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23Demo/sqldelight",
                "../MultiplatformProject/src/androidLibRelease/sqldelight",
                "../MultiplatformProject/src/commonMain/sqldelight")),
        SqlDelightCompilationUnit(
            name = "minApi23DemoSqldelight",
            sourceFolders = listOf(
                "src/main/sqldelight",
                "src/demo/sqldelight",
                "src/minApi23/sqldelight",
                "src/minApi23Demo/sqldelight",
                "src/sqldelight/sqldelight",
                "src/minApi23DemoSqldelight/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23DemoSqldelight/sqldelight",
                "../MultiplatformProject/src/androidLibMain/sqldelight",
                "../MultiplatformProject/src/androidLibDemo/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23Demo/sqldelight",
                "../MultiplatformProject/src/androidLibSqldelight/sqldelight",
                "../MultiplatformProject/src/commonMain/sqldelight")),
        SqlDelightCompilationUnit(
            name = "minApi23FullDebug",
            sourceFolders = listOf(
                "src/main/sqldelight",
                "src/full/sqldelight",
                "src/minApi23/sqldelight",
                "src/minApi23Full/sqldelight",
                "src/debug/sqldelight",
                "src/minApi23FullDebug/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23FullDebug/sqldelight",
                "../MultiplatformProject/src/androidLibMain/sqldelight",
                "../MultiplatformProject/src/androidLibFull/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23Full/sqldelight",
                "../MultiplatformProject/src/androidLibDebug/sqldelight",
                "../MultiplatformProject/src/commonMain/sqldelight")),
        SqlDelightCompilationUnit(
            name = "minApi23FullRelease",
            sourceFolders = listOf(
                "src/main/sqldelight",
                "src/full/sqldelight",
                "src/minApi23/sqldelight",
                "src/minApi23Full/sqldelight",
                "src/release/sqldelight",
                "src/minApi23FullRelease/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23FullRelease/sqldelight",
                "../MultiplatformProject/src/androidLibMain/sqldelight",
                "../MultiplatformProject/src/androidLibFull/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23Full/sqldelight",
                "../MultiplatformProject/src/androidLibRelease/sqldelight",
                "../MultiplatformProject/src/commonMain/sqldelight")),
        SqlDelightCompilationUnit(
            name = "minApi23FullSqldelight",
            sourceFolders = listOf(
                "src/main/sqldelight",
                "src/full/sqldelight",
                "src/minApi23/sqldelight",
                "src/minApi23Full/sqldelight",
                "src/sqldelight/sqldelight",
                "src/minApi23FullSqldelight/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23FullSqldelight/sqldelight",
                "../MultiplatformProject/src/androidLibMain/sqldelight",
                "../MultiplatformProject/src/androidLibFull/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi23Full/sqldelight",
                "../MultiplatformProject/src/androidLibSqldelight/sqldelight",
                "../MultiplatformProject/src/commonMain/sqldelight")),
        SqlDelightCompilationUnit(
            name = "minApi21DemoDebug",
            sourceFolders = listOf(
                "src/main/sqldelight",
                "src/demo/sqldelight",
                "src/minApi21/sqldelight",
                "src/minApi21Demo/sqldelight",
                "src/debug/sqldelight",
                "src/minApi21DemoDebug/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21DemoDebug/sqldelight",
                "../MultiplatformProject/src/androidLibMain/sqldelight",
                "../MultiplatformProject/src/androidLibDemo/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21Demo/sqldelight",
                "../MultiplatformProject/src/androidLibDebug/sqldelight",
                "../MultiplatformProject/src/commonMain/sqldelight")),
        SqlDelightCompilationUnit(
            name = "minApi21DemoRelease",
            sourceFolders = listOf(
                "src/main/sqldelight",
                "src/demo/sqldelight",
                "src/minApi21/sqldelight",
                "src/minApi21Demo/sqldelight",
                "src/release/sqldelight",
                "src/minApi21DemoRelease/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21DemoRelease/sqldelight",
                "../MultiplatformProject/src/androidLibMain/sqldelight",
                "../MultiplatformProject/src/androidLibDemo/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21Demo/sqldelight",
                "../MultiplatformProject/src/androidLibRelease/sqldelight",
                "../MultiplatformProject/src/commonMain/sqldelight")),
        SqlDelightCompilationUnit(
            name = "minApi21DemoSqldelight",
            sourceFolders = listOf("src/main/sqldelight",
                "src/demo/sqldelight",
                "src/minApi21/sqldelight",
                "src/minApi21Demo/sqldelight",
                "src/sqldelight/sqldelight",
                "src/minApi21DemoSqldelight/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21DemoSqldelight/sqldelight",
                "../MultiplatformProject/src/androidLibMain/sqldelight",
                "../MultiplatformProject/src/androidLibDemo/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21Demo/sqldelight",
                "../MultiplatformProject/src/androidLibSqldelight/sqldelight",
                "../MultiplatformProject/src/commonMain/sqldelight")),
        SqlDelightCompilationUnit(
            name = "minApi21FullDebug",
            sourceFolders = listOf(
                "src/main/sqldelight",
                "src/full/sqldelight",
                "src/minApi21/sqldelight",
                "src/minApi21Full/sqldelight",
                "src/debug/sqldelight",
                "src/minApi21FullDebug/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21FullDebug/sqldelight",
                "../MultiplatformProject/src/androidLibMain/sqldelight",
                "../MultiplatformProject/src/androidLibFull/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21Full/sqldelight",
                "../MultiplatformProject/src/androidLibDebug/sqldelight",
                "../MultiplatformProject/src/commonMain/sqldelight")),
        SqlDelightCompilationUnit(
            name = "minApi21FullRelease",
            sourceFolders = listOf(
                "src/main/sqldelight",
                "src/full/sqldelight",
                "src/minApi21/sqldelight",
                "src/minApi21Full/sqldelight",
                "src/release/sqldelight",
                "src/minApi21FullRelease/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21FullRelease/sqldelight",
                "../MultiplatformProject/src/androidLibMain/sqldelight",
                "../MultiplatformProject/src/androidLibFull/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21Full/sqldelight",
                "../MultiplatformProject/src/androidLibRelease/sqldelight",
                "../MultiplatformProject/src/commonMain/sqldelight")),
        SqlDelightCompilationUnit(
            name = "minApi21FullSqldelight",
            sourceFolders = listOf(
                "src/main/sqldelight",
                "src/full/sqldelight",
                "src/minApi21/sqldelight",
                "src/minApi21Full/sqldelight",
                "src/sqldelight/sqldelight",
                "src/minApi21FullSqldelight/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21FullSqldelight/sqldelight",
                "../MultiplatformProject/src/androidLibMain/sqldelight",
                "../MultiplatformProject/src/androidLibFull/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21/sqldelight",
                "../MultiplatformProject/src/androidLibMinApi21Full/sqldelight",
                "../MultiplatformProject/src/androidLibSqldelight/sqldelight",
                "../MultiplatformProject/src/commonMain/sqldelight"))
    )

    propertiesFile.delete()
  }
}