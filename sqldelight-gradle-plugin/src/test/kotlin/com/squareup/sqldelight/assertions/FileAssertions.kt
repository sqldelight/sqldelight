package com.squareup.sqldelight.assertions

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.Stack

internal class FileSubject private constructor(
  metadata: FailureMetadata,
  actual: File
) : Subject<FileSubject, File>(metadata, actual) {
  fun exists() {
    assertThat(actual().exists()).named("File ${actual()} exists").isTrue()
  }

  fun contentsAreEqualTo(other: File) {
    assertThat(actual()).exists()
    assertThat(other).exists()
    val buildOutput = Stack<File>().apply {
      actual().listFiles()!!.forEach { push(it) }
    }
    val expectedOutput = Stack<File>().apply {
      other.listFiles()!!.forEach { push(it) }
    }

    while (buildOutput.isNotEmpty() || expectedOutput.isNotEmpty()) {
      val output = if (buildOutput.isEmpty()) {
        throw AssertionError("Did not build a file ${expectedOutput.pop().name}")
      } else {
        buildOutput.pop()
      }
      val expected = if (expectedOutput.isEmpty()) {
        throw AssertionError("Expected a file ${output.name}")
      } else {
        expectedOutput.pop()
      }

      assertThat(output.name).isEqualTo(expected.name)
      assertThat(output.isDirectory).isEqualTo(expected.isDirectory)
      if (!output.isDirectory) {
        assertThat(output.readText())
          .named("Expected file ${output.path} to equal file ${expected.path}")
          .isEqualTo(expected.readText())
      } else {
        output.listFiles()!!.forEach { buildOutput.push(it) }
        expected.listFiles()!!.forEach { expectedOutput.push(it) }
      }
    }
  }

  companion object {
    private val FILE_SUBJECT_FACTORY = Factory<FileSubject, File> { metadata, actual ->
      FileSubject(metadata, actual)
    }

    fun assertThat(file: File): FileSubject {
      return assertAbout(FILE_SUBJECT_FACTORY).that(file)
    }
  }
}
