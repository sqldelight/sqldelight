package com.squareup.sqldelight

import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitResult.CONTINUE
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class SqlDelightPluginTest {
  @get:Rule val fixture = FixtureRunner("-Dsqldelight.skip.runtime=true")

  @FixtureName("works-fine")
  @Test
  fun worksFine() {
    val result = fixture.execute()
    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  @FixtureName("works-fine-as-library")
  @Test
  fun worksFineAsLibrary() {
    val result = fixture.execute()

    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  @FixtureName("key-value-works-fine")
  @Test
  fun keyValueWorksFine() {
    val result = fixture.execute()

    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  @FixtureName("unknown-class-type")
  @Test
  fun unknownClassType() {
    val result = fixture.executeAndFail()

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
    val result = fixture.executeAndFail()

    assertThat(result.standardError).contains(
        "Table.sq line 1:0 - mismatched input 'CREATE' expecting {<EOF>, K_PACKAGE, UNEXPECTED_CHAR}")
  }

  @FixtureName("syntax-error")
  @Test
  fun syntaxError() {
    val result = fixture.executeAndFail()

    assertThat(result.standardError).contains(
        "Table.sq line 5:0 - mismatched input 'FRM' expecting {';', ',', K_EXCEPT, K_FROM, K_GROUP, K_INTERSECT, K_LIMIT, K_ORDER, K_UNION, K_WHERE}")
  }

  @FixtureName("unknown-type")
  @Test
  fun unknownType() {
    val result = fixture.executeAndFail()

    assertThat(result.standardError).contains(
        "Table.sq line 5:15 - no viable alternative at input 'LIST'")
  }

  @FixtureName("nullable-enum")
  @Test
  fun nullableEnum() {
    val result = fixture.execute()

    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  @FixtureName("nullable-boolean")
  @Test
  fun nullableBoolean() {
    val result = fixture.execute()

    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  @Ignore("https://github.com/square/sqldelight/issues/80")
  @FixtureName("works-for-kotlin")
  @Test
  fun worksForKotlin() {
    val result = fixture.execute()

    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  @FixtureName("custom-class-works-fine")
  @Test
  fun customClassWorksFine() {
    val result = fixture.execute()

    assertThat(result.standardOutput).contains("BUILD SUCCESSFUL")
    assertExpectedFiles()
  }

  private fun assertExpectedFiles() {
    val expectedDir = File(fixture.root(), "expected/").toPath()
    val outputDir = File(fixture.root(), "build/generated/source/sqldelight/").toPath()
    Files.walkFileTree(expectedDir, object : SimpleFileVisitor<Path>() {
      override fun visitFile(expectedFile: Path, attrs: BasicFileAttributes): FileVisitResult {
        val relative = expectedDir.relativize(expectedFile).toString()
        val actualFile = outputDir.resolve(relative)
        if (!Files.exists(actualFile)) {
          throw AssertionError("Expected file not found: $actualFile")
        }

        val expected = String(Files.readAllBytes(expectedFile), UTF_8)
        val actual = String(Files.readAllBytes(actualFile), UTF_8)
        assertThat(actual).named(relative).isEqualTo(expected)

        return CONTINUE
      }
    })
  }
}
