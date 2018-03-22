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