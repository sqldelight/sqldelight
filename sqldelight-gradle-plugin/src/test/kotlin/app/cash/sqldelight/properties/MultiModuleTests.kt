package app.cash.sqldelight.properties

import app.cash.sqldelight.gradle.SqlDelightCompilationUnitImpl
import app.cash.sqldelight.gradle.SqlDelightSourceFolderImpl
import app.cash.sqldelight.properties
import app.cash.sqldelight.withCommonConfiguration
import app.cash.sqldelight.withInvariantPathSeparators
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Ignore
import org.junit.Test

class MultiModuleTests {
  @Test
  fun `sqldelight dependencies are added to the compilation unit`() {
    var fixtureRoot = File("src/test/multi-module").absoluteFile

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true")
      .build()

    // verify
    fixtureRoot = File(fixtureRoot, "ProjectA")
    val properties = properties(fixtureRoot).databases.single().withInvariantPathSeparators()
    assertThat(properties.packageName).isEqualTo("com.example")
    assertThat(properties.compilationUnits).hasSize(1)

    with(properties.compilationUnits[0]) {
      assertThat(outputDirectoryFile).isEqualTo(File(fixtureRoot, "build/generated/sqldelight/code/Database/main"))
      assertThat(sourceFolders).containsExactly(
        SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
        SqlDelightSourceFolderImpl(File(fixtureRoot, "../ProjectB/src/main/sqldelight"), true),
      )
    }
  }

  @Test
  fun integrationTests() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/multi-module"))
      .withArguments("clean", ":ProjectA:check", "--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `android multi module integration tests`() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/multi-module"))
      .withArguments("clean", ":AndroidProject:connectedCheck", "--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `the android target of a multiplatform module is a dependency for an android only module`() {
    var fixtureRoot = File("src/test/multi-module").absoluteFile

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true")
      .forwardOutput()
      .build()

    fixtureRoot = File(fixtureRoot, "AndroidProject")

    val properties = properties(fixtureRoot).databases.single().withInvariantPathSeparators()

    assertThat(properties.packageName).isEqualTo("com.sample.android")
    assertThat(properties.compilationUnits).containsExactly(
      SqlDelightCompilationUnitImpl(
        name = "minApi23Debug",
        sourceFolders = setOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Debug/sqldelight"), false),
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23Debug"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi23Release",
        sourceFolders = setOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Release/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false),
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23Release"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi23Sqldelight",
        sourceFolders = setOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Sqldelight/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false),
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23Sqldelight"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi21Debug",
        sourceFolders = setOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Debug/sqldelight"), false),
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21Debug"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi21Release",
        sourceFolders = setOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Release/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false),
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21Release"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi21Sqldelight",
        sourceFolders = setOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Sqldelight/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false),
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21Sqldelight"),
      ),
    )
  }

  @Test
  fun `diamond dependency is correctly resolved`() {
    var fixtureRoot = File("src/test/diamond-dependency").absoluteFile

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true")
      .forwardOutput()
      .build()

    fixtureRoot = File(fixtureRoot, "app")

    val properties = properties(fixtureRoot).databases.single().withInvariantPathSeparators()

    assertThat(properties.packageName).isEqualTo("com.example.app")
    assertThat(properties.compilationUnits).containsExactly(
      SqlDelightCompilationUnitImpl(
        name = "main",
        sourceFolders = setOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../bottom/src/main/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../middleA/src/main/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../middleB/src/main/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
        ),
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/Database/main"),
      ),
    )
  }

  @Test
  fun `the same package for two modules is not allowed`() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/multi-project-duplicated-package"))
      .withArguments("clean", ":app:assemble", "--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true")

    val result = runner.buildAndFail()
    assertThat(result.output).contains("The package 'com.example.bottom' is defined in multiple projects [':bottomB', ':bottomA'], which are used in the project ':app'")
  }

  @Test
  fun `the same package for two direct connected modules is not allowed`() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/multi-project-duplicated-package-direct-dependency"))
      .withArguments("clean", ":app:assemble", "--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true")

    val result = runner.buildAndFail()
    assertThat(result.output).contains("The package 'com.example.app' is defined in multiple projects [':app', ':bottom'], which are used in the project ':app'")
  }

  @Test
  fun `diamond dependency is buildable`() {
    val fixtureRoot = File("src/test/diamond-dependency").absoluteFile

    val runner = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", ":app:assemble", "--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true")
      .forwardOutput()

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `dependency adapter is correctly resolved`() {
    val fixtureRoot = File("src/test/dependency-adapter").absoluteFile

    val runner = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", ":moduleA:check", "--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true")
      .forwardOutput()

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `dependency adapter with migration is correctly resolved`() {
    val fixtureRoot = File("src/test/dependency-adapter-migration").absoluteFile

    val runner = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", ":moduleA:check", "--stacktrace", "-Dorg.gradle.unsafe.isolated-projects=true")
      .forwardOutput()
      .withDebug(true)

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
}
