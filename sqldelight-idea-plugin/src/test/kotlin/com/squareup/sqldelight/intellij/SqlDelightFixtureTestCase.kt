/*
 * Copyright (C) 2018 Square, Inc.
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

package com.squareup.sqldelight.intellij

import com.intellij.psi.PsiDirectory
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.testFramework.registerServiceInstance
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.lang.SqlDelightFile

abstract class SqlDelightFixtureTestCase : LightCodeInsightFixtureTestCase() {
  protected val sqldelightDir = "main/sqldelight/com/sample"

  open val fixtureDirectory: String = ""

  override fun getTestDataPath() = "testData/$fixtureDirectory"

  override fun setUp() {
    super.setUp()
    myModule.registerServiceInstance(SqlDelightFileIndex::class.java, FileIndex())
  }

  inner class FileIndex : SqlDelightFileIndex {
    override val isConfigured = true
    override val outputDirectory = ""
    override val packageName = "com.example"
    override fun packageName(file: SqlDelightFile) = "com.example"

    override fun sourceFolders(file: SqlDelightFile?): List<PsiDirectory> {
      return listOf(myFixture.file.parent!!)
    }
  }
}