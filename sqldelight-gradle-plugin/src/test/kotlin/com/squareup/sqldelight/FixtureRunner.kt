/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight

import com.google.common.io.Resources
import org.gradle.testkit.runner.GradleRunner
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.util.Properties

class FixtureRunner() : TestRule {
  private lateinit var root: File
  private lateinit var runner: GradleRunner

  fun root() = root

  fun execute(vararg args: String = arrayOf("build")) = runnerWithArgs(*args).build()
  fun executeAndFail(vararg args: String = arrayOf("build")) = runnerWithArgs(*args).buildAndFail();

  private fun runnerWithArgs(vararg args: String) = runner
      .withArguments(listOf("clean", "--stacktrace") + args)

  override fun apply(base: Statement, description: Description): Statement {
    val annotation = description.getAnnotation(FixtureName::class.java) ?:
        throw IllegalStateException(
            "Test '${description.displayName}' missing @FixtureName annotation.")
    val name = annotation.value

    root = File("src/test/fixtures/$name")
    if (!root.exists()) {
      throw IllegalStateException("Fixture $root does not exist.")
    }

    val androidHome = androidHome()
    File(root, "local.properties").writeText("sdk.dir=$androidHome\n")

    val pluginClasspathUrl = Resources.getResource("plugin-classpath.txt") ?:
        throw IllegalStateException(
            "Did not find plugin classpath resource, run `testClasses` build task.")
    val pluginClasspath = Resources.readLines(pluginClasspathUrl, Charsets.UTF_8).map(::File)

    runner = GradleRunner.create()
        .withProjectDir(root)
        .withPluginClasspath(pluginClasspath)

    return base
  }

  private fun androidHome(): String {
    val env = System.getenv("ANDROID_HOME")
    if (env != null) {
      return env
    }
    val localProp = File(File(System.getProperty("user.dir")).parentFile, "local.properties")
    if (localProp.exists()) {
      val prop = Properties()
      localProp.inputStream().use {
        prop.load(it)
      }
      val sdkHome = prop.getProperty("sdk.dir")
      if (sdkHome != null) {
        return sdkHome
      }
    }
    throw IllegalStateException(
        "Missing 'ANDROID_HOME' environment variable or local.properties with 'sdk.dir'")
  }
}
