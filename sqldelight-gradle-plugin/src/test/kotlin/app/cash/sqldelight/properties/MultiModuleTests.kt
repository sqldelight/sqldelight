package app.cash.sqldelight.properties

import app.cash.sqldelight.gradle.SqlDelightCompilationUnitImpl
import app.cash.sqldelight.gradle.SqlDelightSourceFolderImpl
import app.cash.sqldelight.properties
import app.cash.sqldelight.withCommonConfiguration
import app.cash.sqldelight.withInvariantPathSeparators
import app.cash.sqldelight.withSortedCompilationUnits
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Ignore
import org.junit.Test

class MultiModuleTests {
  @Ignore
  @Test
  fun `sqldelight dependencies are added to the compilation unit`() {
    var fixtureRoot = File("src/test/multi-module").absoluteFile

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "--stacktrace")
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

  @Ignore
  @Test
  fun integrationTests() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/multi-module"))
      .withArguments("clean", ":ProjectA:check", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Ignore
  @Test
  fun `android multi module integration tests`() {
    val runner = GradleRunner.create()
      .withCommonConfiguration(File("src/test/multi-module"))
      .withArguments("clean", ":AndroidProject:connectedCheck", "--stacktrace")

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Ignore
  @Test
  fun `the android target of a multiplatform module is a dependency for an android only module`() {
    var fixtureRoot = File("src/test/multi-module").absoluteFile

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "--stacktrace")
      .forwardOutput()
      .build()

    fixtureRoot = File(fixtureRoot, "AndroidProject")

    val properties = properties(fixtureRoot).databases.single().withInvariantPathSeparators()
      .withSortedCompilationUnits()
    assertThat(properties.packageName).isEqualTo("com.sample.android")
    assertThat(properties.compilationUnits).containsExactly(
      SqlDelightCompilationUnitImpl(
        name = "minApi23Debug",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Debug/sqldelight"), false),
        ).sortedBy { it.folder.absolutePath },
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23Debug"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi23Release",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Release/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false),
        ).sortedBy { it.folder.absolutePath },
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23Release"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi23Sqldelight",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi23Sqldelight/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false),
        ).sortedBy { it.folder.absolutePath },
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi23Sqldelight"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi21Debug",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/debug/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Debug/sqldelight"), false),
        ).sortedBy { it.folder.absolutePath },
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21Debug"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi21Release",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Release/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/release/sqldelight"), false),
        ).sortedBy { it.folder.absolutePath },
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21Release"),
      ),
      SqlDelightCompilationUnitImpl(
        name = "minApi21Sqldelight",
        sourceFolders = listOf(
          SqlDelightSourceFolderImpl(File(fixtureRoot, "../MultiplatformProject/src/commonMain/sqldelight"), true),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/main/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/minApi21Sqldelight/sqldelight"), false),
          SqlDelightSourceFolderImpl(File(fixtureRoot, "src/sqldelight/sqldelight"), false),
        ).sortedBy { it.folder.absolutePath },
        outputDirectoryFile = File(fixtureRoot, "build/generated/sqldelight/code/CommonDb/minApi21Sqldelight"),
      ),
    )
  }

  @Test
  fun `diamond dependency is correctly resolved`() {
    var fixtureRoot = File("src/test/diamond-dependency").absoluteFile

    GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", "--stacktrace")
      .forwardOutput()
      .build()

    fixtureRoot = File(fixtureRoot, "app")

    val properties = properties(fixtureRoot).databases.single().withInvariantPathSeparators()
      .withSortedCompilationUnits()
    assertThat(properties.packageName).isEqualTo("com.example.app")
    assertThat(properties.compilationUnits).containsExactly(
      SqlDelightCompilationUnitImpl(
        name = "main",
        sourceFolders = listOf(
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
  fun `dependency adapter is correctly resolved`() {
    val fixtureRoot = File("src/test/dependency-adapter").absoluteFile

    val runner = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", ":moduleA:check", "--stacktrace")
      .forwardOutput()

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }

  @Test
  fun `dependency adapter with migration is correctly resolved`() {
    val fixtureRoot = File("src/test/dependency-adapter-migration").absoluteFile

    val runner = GradleRunner.create()
      .withCommonConfiguration(fixtureRoot)
      .withArguments("clean", ":moduleA:check", "--stacktrace")
      .forwardOutput()
      .withDebug(true)

    val result = runner.build()
    assertThat(result.output).contains("BUILD SUCCESSFUL")
  }
}
