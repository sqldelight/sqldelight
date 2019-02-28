package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.SqlDelightCompilationUnit
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.core.SqlDelightSourceFolder
import org.gradle.testkit.runner.GradleRunner
import org.junit.Ignore
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
      assertThat(sourceFolders).containsExactly(SqlDelightSourceFolder("src/main/sqldelight", false), SqlDelightSourceFolder("../ProjectB/src/main/sqldelight", true))
    }

    propertiesFile.delete()
  }

  @Test
  @Ignore // Fixing...
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
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/demo/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23Demo/sqldelight", false),
                SqlDelightSourceFolder("src/debug/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23DemoDebug/sqldelight", false),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23DemoDebug/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibDemo/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23Demo/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibDebug/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true))),
        SqlDelightCompilationUnit(
            name = "minApi23DemoRelease",
            sourceFolders = listOf(
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/demo/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23Demo/sqldelight", false),
                SqlDelightSourceFolder("src/release/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23DemoRelease/sqldelight", false),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23DemoRelease/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibDemo/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23Demo/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibRelease/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true))),
        SqlDelightCompilationUnit(
            name = "minApi23DemoSqldelight",
            sourceFolders = listOf(
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/demo/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23Demo/sqldelight", false),
                SqlDelightSourceFolder("src/sqldelight/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23DemoSqldelight/sqldelight", false),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23DemoSqldelight/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibDemo/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23Demo/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibSqldelight/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true))),
        SqlDelightCompilationUnit(
            name = "minApi23FullDebug",
            sourceFolders = listOf(
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/full/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23Full/sqldelight", false),
                SqlDelightSourceFolder("src/debug/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23FullDebug/sqldelight", false),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23FullDebug/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibFull/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23Full/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibDebug/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true))),
        SqlDelightCompilationUnit(
            name = "minApi23FullRelease",
            sourceFolders = listOf(
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/full/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23Full/sqldelight", false),
                SqlDelightSourceFolder("src/release/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23FullRelease/sqldelight", false),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23FullRelease/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibFull/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23Full/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibRelease/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true))),
        SqlDelightCompilationUnit(
            name = "minApi23FullSqldelight",
            sourceFolders = listOf(
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/full/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23Full/sqldelight", false),
                SqlDelightSourceFolder("src/sqldelight/sqldelight", false),
                SqlDelightSourceFolder("src/minApi23FullSqldelight/sqldelight", false),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23FullSqldelight/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibFull/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi23Full/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibSqldelight/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true))),
        SqlDelightCompilationUnit(
            name = "minApi21DemoDebug",
            sourceFolders = listOf(
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/demo/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21Demo/sqldelight", false),
                SqlDelightSourceFolder("src/debug/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21DemoDebug/sqldelight", false),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21DemoDebug/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibDemo/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21Demo/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibDebug/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true))),
        SqlDelightCompilationUnit(
            name = "minApi21DemoRelease",
            sourceFolders = listOf(
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/demo/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21Demo/sqldelight", false),
                SqlDelightSourceFolder("src/release/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21DemoRelease/sqldelight", false),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21DemoRelease/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibDemo/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21Demo/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibRelease/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true))),
        SqlDelightCompilationUnit(
            name = "minApi21DemoSqldelight",
            sourceFolders = listOf(SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/demo/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21Demo/sqldelight", false),
                SqlDelightSourceFolder("src/sqldelight/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21DemoSqldelight/sqldelight", false),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21DemoSqldelight/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibDemo/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21Demo/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibSqldelight/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true))),
        SqlDelightCompilationUnit(
            name = "minApi21FullDebug",
            sourceFolders = listOf(
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/full/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21Full/sqldelight", false),
                SqlDelightSourceFolder("src/debug/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21FullDebug/sqldelight", false),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21FullDebug/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibFull/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21Full/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibDebug/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true))),
        SqlDelightCompilationUnit(
            name = "minApi21FullRelease",
            sourceFolders = listOf(
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/full/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21Full/sqldelight", false),
                SqlDelightSourceFolder("src/release/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21FullRelease/sqldelight", false),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21FullRelease/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibFull/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21Full/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibRelease/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true))),
        SqlDelightCompilationUnit(
            name = "minApi21FullSqldelight",
            sourceFolders = listOf(
                SqlDelightSourceFolder("src/main/sqldelight", false),
                SqlDelightSourceFolder("src/full/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21Full/sqldelight", false),
                SqlDelightSourceFolder("src/sqldelight/sqldelight", false),
                SqlDelightSourceFolder("src/minApi21FullSqldelight/sqldelight", false),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21FullSqldelight/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMain/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibFull/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibMinApi21Full/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/androidLibSqldelight/sqldelight", true),
                SqlDelightSourceFolder("../MultiplatformProject/src/commonMain/sqldelight", true)))
    )

    propertiesFile.delete()
  }
}