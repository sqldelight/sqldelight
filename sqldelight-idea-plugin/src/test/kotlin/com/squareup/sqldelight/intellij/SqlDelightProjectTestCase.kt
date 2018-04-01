package com.squareup.sqldelight.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
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
    super.setUp()
    myFixture.copyDirectoryToProject("", "")
    SqlDelightFileIndex.setInstance(myModule, FileIndex(configurePropertiesFile(), tempRoot))
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
    return PsiTreeUtil.collectElements(file) {
      it is LeafPsiElement && it.text == text
    }.mapNotNull { it.getNonStrictParentOfType<T>() }
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