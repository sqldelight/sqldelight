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

import com.alecstrong.sql.psi.core.DialectPreset
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.squareup.sqldelight.core.SqlDelightDatabaseName
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqldelightParserUtil
import com.squareup.sqldelight.core.lang.SqlDelightFile

abstract class SqlDelightFixtureTestCase : LightCodeInsightFixtureTestCase() {
  protected val sqldelightDir = "main/sqldelight/com/sample"

  open val fixtureDirectory: String = ""

  override fun getTestDataPath() = "testData/$fixtureDirectory"

  override fun setUp() {
    super.setUp()
    DialectPreset.SQLITE_3_18.setup()
    SqldelightParserUtil.overrideSqlParser()
    SqlDelightFileIndex.setInstance(module, FileIndex())
  }

  inner class FileIndex : SqlDelightFileIndex {
    override val isConfigured = true
    override val packageName = "com.example"
    override val className = "MyDatabase"
    override fun packageName(file: SqlDelightFile) = "com.example"
    override val contentRoot = module.rootManager.contentRoots.single()
    override val outputDirectory = ""
    override val dependencies = emptyList<SqlDelightDatabaseName>()

    override fun sourceFolders(
      file: SqlDelightFile,
      includeDependencies: Boolean
    ): List<PsiDirectory> {
      return listOf(myFixture.file.parent!!)
    }

    override fun sourceFolders(
      file: VirtualFile,
      includeDependencies: Boolean
    ): Collection<VirtualFile> {
      return listOf(module.rootManager.contentRoots.first())
    }
  }
}
