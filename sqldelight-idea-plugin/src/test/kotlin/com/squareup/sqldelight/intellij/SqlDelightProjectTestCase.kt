package com.squareup.sqldelight.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.project.rootManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.testFramework.registerServiceInstance
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.intellij.util.GeneratedVirtualFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.io.File
import java.io.PrintStream

abstract class SqlDelightProjectTestCase : LightCodeInsightFixtureTestCase() {
  protected val tempRoot: VirtualFile
    get() = myModule.rootManager.contentRoots.single()
  override fun setUp() {
    configurePropertiesFile().toFile(File(testDataPath, SqlDelightPropertiesFile.NAME))

    super.setUp()
    myFixture.copyDirectoryToProject("", "")
    myModule.registerServiceInstance(SqlDelightFileIndex::class.java, FileIndex(myModule))
    ApplicationManager.getApplication().runWriteAction {
      generateSqlDelightFiles()
    }
  }

  override fun tearDown() {
    super.tearDown()
    File(testDataPath, SqlDelightPropertiesFile.NAME).delete()
  }

  override fun getTestDataPath() = "testData/project"

  open fun configurePropertiesFile(): SqlDelightPropertiesFile {
    return SqlDelightPropertiesFile(
        packageName = "com.example",
        sourceSets = listOf(
            listOf("src/main/sqldelight", "src/internal/sqldelight", "src/debug/sqldelight", "src/internalDebug/sqldelight"),
            listOf("src/main/sqldelight", "src/internal/sqldelight", "src/release/sqldelight", "src/internalRelease/sqldelight"),
            listOf("src/main/sqldelight", "src/production/sqldelight", "src/debug/sqldelight", "src/productionDebug/sqldelight"),
            listOf("src/main/sqldelight", "src/production/sqldelight", "src/release/sqldelight", "src/productionRelease/sqldelight")
        ),
        outputDirectory = "build"
    )
  }

  protected inline fun <reified T: PsiElement> searchForElement(text: String): Collection<T> {
    var offset = file.text.indexOf(text)
    val result = mutableListOf<T>()
    while (offset != -1) {
      val element = file.findElementAt(offset)!!
      offset = file.text.indexOf(text, offset + 1)
      result.add(element.getNonStrictParentOfType() ?: continue)
    }
    return result
  }

  private fun generateSqlDelightFiles() {
    val mainDir = myModule.rootManager.contentRoots.single().findFileByRelativePath("src/main")!!
    val virtualFileWriter = { filePath: String ->
      val vFile: VirtualFile by GeneratedVirtualFile(filePath, myModule)
      PrintStream(vFile.getOutputStream(this))
    }
    var fileToGenerateDb: SqlDelightFile? = null
    myModule.rootManager.fileIndex.iterateContentUnderDirectory(mainDir) { file ->
      if (file.fileType != SqlDelightFileType) return@iterateContentUnderDirectory true
      val sqlFile = (psiManager.findFile(file)!! as SqlDelightFile)
      sqlFile.viewProvider.contentsSynchronized()
      fileToGenerateDb = sqlFile
      return@iterateContentUnderDirectory true
    }
    SqlDelightCompiler.writeQueryWrapperFile(myModule, fileToGenerateDb!!, virtualFileWriter)
  }
}