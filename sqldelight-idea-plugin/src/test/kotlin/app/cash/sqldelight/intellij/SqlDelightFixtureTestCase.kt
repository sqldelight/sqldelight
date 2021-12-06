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

package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.SqldelightParserUtil
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.intellij.gradle.FileIndexMap
import com.alecstrong.sql.psi.core.DialectPreset
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

abstract class SqlDelightFixtureTestCase : LightJavaCodeInsightFixtureTestCase() {
  protected val sqldelightDir = "main/sqldelight/com/sample"

  open val fixtureDirectory: String = ""

  override fun getTestDataPath() = "testData/$fixtureDirectory"

  override fun setUp() {
    super.setUp()
    DialectPreset.SQLITE_3_18.setup()
    SqldelightParserUtil.overrideSqlParser()
    FileIndexMap.defaultIndex = LightFileIndex()
  }

  inner class LightFileIndex : SqlDelightFileIndex {
    override val isConfigured = true
    override val packageName = "com.example"
    override val className = "MyDatabase"
    override fun packageName(file: SqlDelightFile) = "com.example"
    override val contentRoot = module.rootManager.contentRoots.single()
    override val dependencies = emptyList<SqlDelightDatabaseName>()
    override val deriveSchemaFromMigrations = false

    override fun outputDirectory(file: SqlDelightFile) = outputDirectories()
    override fun outputDirectories() = listOf("")

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
