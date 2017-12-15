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

import com.google.common.truth.Truth.assertThat
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

class BuildFilesRule(private val root: File) : TestRule {
  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        val buildFile = File(root, "build.gradle")
        val hasBuildFile = buildFile.exists()
        if (hasBuildFile) {
          assertThat(buildFile.readText())
        } else {
          val buildFileTemplate = File(root, "../../build.gradle").readText()
          buildFile.writeText(buildFileTemplate)
        }

        val manifestFile = File(root, "src/main/AndroidManifest.xml")
        val hasManifestFile = manifestFile.exists()
        if (!hasManifestFile) {
          val manifestFileTemplate = File(root, "../../AndroidManifest.xml").readText()
          manifestFile.writeText(manifestFileTemplate)
        }

        try {
          base.evaluate()
        } finally {
          if (!hasBuildFile) buildFile.delete()
          if (!hasManifestFile) manifestFile.delete()
        }
      }
    }
  }
}
