package com.squareup.sqldelight

import com.google.common.io.Files
import com.google.common.io.Resources
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class SqlDelightPluginTest {
  @get:Rule val fixture = TemporaryFixture()

  private val gradleRunner = GradleRunner.create()

  private var pluginClasspath: List<File>? = null

  @Before
  fun setup() {
    val pluginClasspathResource = javaClass.classLoader.getResource("plugin-classpath.txt") ?:
        throw IllegalStateException(
            "Did not find plugin classpath resource, run `testClasses` build task.")

    pluginClasspath = Resources.readLines(pluginClasspathResource, UTF_8).map { File(it) }

    val studioProperties = File(System.getProperty("user.dir") + "/..", "local.properties")
    if (!studioProperties.exists()) {
      throw IllegalStateException("Need a local.properties file with sdk.dir to run tests, "
          + "open this project in Android Studio to have a local.properties automatically generated")
    }
    val localProperties = File(fixture.root, "local.properties")
    Files.copy(studioProperties, localProperties)
  }

  @FixtureName("works-fine")
  @Test
  fun worksFine() {
    val result = gradleRunner.withProjectDir(fixture.root).withArguments("assembleDebug",
        "--stacktrace").withPluginClasspath(pluginClasspath).build()

    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  @FixtureName("works-fine-as-library")
  @Test
  fun worksFineAsLibrary() {
    val result = gradleRunner.withProjectDir(fixture.root).withArguments(
        "compileDebugJavaWithJavac", "--stacktrace").withPluginClasspath(pluginClasspath).build()

    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  @FixtureName("key-value-works-fine")
  @Test
  fun keyValueWorksFine() {
    val result = gradleRunner.withProjectDir(fixture.root).withArguments("assembleDebug",
        "--stacktrace").withPluginClasspath(pluginClasspath).build()

    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  @FixtureName("unknown-class-type")
  @Test
  fun unknownClassType() {
    val result = prepareTask().buildAndFail()

    assertThat(result.standardError).contains(
        "Table.sq line 9:2 - Couldnt make a guess for type of colum a_class\n"
            + "  07\t\tCREATE TABLE test (\n"
            + "  08\t\t  id INT PRIMARY KEY NOT NULL,\n"
            + "  09\t\t  a_class CLASS('')\n"
            + "  \t\t    ^^^^^^^^^^^^^^^^^\n"
            + "  10\t\t)")
  }

  @FixtureName("missing-package-statement")
  @Test
  fun missingPackageStatement() {
    val result = prepareTask().buildAndFail()

    assertThat(result.standardError).contains(
        "Table.sq line 1:0 - mismatched input 'CREATE' expecting {<EOF>, K_PACKAGE, UNEXPECTED_CHAR}")
  }

  @FixtureName("syntax-error")
  @Test
  fun syntaxError() {
    val result = prepareTask().buildAndFail()

    assertThat(result.standardError).contains(
        "Table.sq line 5:0 - mismatched input 'FRM' expecting {';', ',', K_EXCEPT, K_FROM, K_GROUP, K_INTERSECT, K_LIMIT, K_ORDER, K_UNION, K_WHERE}")
  }

  @FixtureName("unknown-type")
  @Test
  fun unknownType() {
    val result = prepareTask().buildAndFail()

    assertThat(result.standardError).contains(
        "Table.sq line 5:15 - no viable alternative at input 'LIST'")
  }

  @FixtureName("nullable-enum")
  @Test
  fun nullableEnum() {
    val result = gradleRunner.withProjectDir(fixture.root).withArguments("assembleDebug",
        "--stacktrace").withPluginClasspath(pluginClasspath).build()

    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  @FixtureName("nullable-boolean")
  @Test
  fun nullableBoolean() {
    val result = gradleRunner.withProjectDir(fixture.root).withArguments("assembleDebug",
        "--stacktrace").withPluginClasspath(pluginClasspath).build()

    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  @FixtureName("works-for-kotlin")
  @Test
  fun worksForKotlin() {
    val result = gradleRunner.withProjectDir(fixture.root).withArguments("assembleDebug",
        "--stacktrace").withPluginClasspath(pluginClasspath).build()

    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  @FixtureName("custom-class-works-fine")
  @Test
  fun customClassWorksFine() {
    val result = gradleRunner.withProjectDir(fixture.root).withArguments("assembleDebug",
        "--stacktrace").withPluginClasspath(pluginClasspath).build()

    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  private fun prepareTask(): GradleRunner {
    return gradleRunner.withProjectDir(fixture.root).withArguments("generateSqliteInterface",
        "--stacktrace").withPluginClasspath(pluginClasspath)
  }

  private fun assertExpectedFiles() {
    val expectedDir = File(fixture.root, "expected/").toPath()
    val outputDir = File(fixture.root, "build/generated/source/sqlite/").toPath()
    java.nio.file.Files.walkFileTree(expectedDir, object : SimpleFileVisitor<Path>() {
      @Throws(IOException::class)
      override fun visitFile(expectedFile: Path, attrs: BasicFileAttributes): FileVisitResult {
        val relative = expectedDir.relativize(expectedFile).toString()
        val actualFile = outputDir.resolve(relative)

        val expected = String(java.nio.file.Files.readAllBytes(expectedFile), UTF_8)
        val actual = String(java.nio.file.Files.readAllBytes(actualFile), UTF_8)
        assertThat(actual).named(relative).isEqualTo(expected)

        return CONTINUE
      }
    })
  }
}
