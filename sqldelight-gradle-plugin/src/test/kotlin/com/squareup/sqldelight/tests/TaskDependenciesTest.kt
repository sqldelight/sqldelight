package com.squareup.sqldelight.tests

import com.google.common.truth.Truth
import com.squareup.sqldelight.withCommonConfiguration
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File

class TaskDependenciesTest {
  @Test
  fun `task dependencies are properly propagated`() {
    val output = GradleRunner.create()
      .withCommonConfiguration(File("src/test/task-dependencies"))
      .withArguments("clean", "checkSources", "--stacktrace")
      .build()

    Truth.assertThat(output.task(":checkSources")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    Truth.assertThat(output.task(":generateMainDatabaseInterface")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
  }
}
