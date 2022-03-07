package app.cash.sqldelight.assertions

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.util.ArrayDeque

internal class FileSubject private constructor(
  metadata: FailureMetadata,
  private val actual: File
) : Subject(metadata, actual) {
  fun exists() {
    assertWithMessage("File $actual exists").that(actual.exists()).isTrue()
  }

  fun contentsAreEqualTo(other: File) {
    assertThat(actual).exists()
    assertThat(other).exists()
    val buildOutput = ArrayDeque<File>().apply {
      actual.listFiles()!!.forEach { addFirst(it) }
    }
    val expectedOutput = ArrayDeque<File>().apply {
      other.listFiles()!!.forEach { addFirst(it) }
    }

    while (buildOutput.isNotEmpty() || expectedOutput.isNotEmpty()) {
      val output = if (buildOutput.isEmpty()) {
        throw AssertionError("Did not build a file ${expectedOutput.removeFirst().name}")
      } else {
        buildOutput.removeFirst()
      }
      val expected = if (expectedOutput.isEmpty()) {
        throw AssertionError("Expected a file ${output.name}")
      } else {
        expectedOutput.removeFirst()
      }

      assertThat(output.name).isEqualTo(expected.name)
      assertThat(output.isDirectory).isEqualTo(expected.isDirectory)
      if (!output.isDirectory) {
        assertWithMessage("Expected file ${output.path} to equal file ${expected.path}")
          .that(output.readText())
          .isEqualTo(expected.readText())
      } else {
        output.listFiles()!!.forEach { buildOutput.addFirst(it) }
        expected.listFiles()!!.forEach { expectedOutput.addFirst(it) }
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
