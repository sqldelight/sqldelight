package com.squareup.sqldelight.intellij

import com.alecstrong.sql.psi.core.DialectPreset
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.squareup.sqldelight.core.SqlDelightCompilationUnit
import com.squareup.sqldelight.core.SqlDelightDatabaseProperties
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightPropertiesFile
import com.squareup.sqldelight.core.SqlDelightSourceFolder
import com.squareup.sqldelight.core.SqldelightParserUtil
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.core.lang.SqlDelightQueriesFile
import com.squareup.sqldelight.intellij.util.GeneratedVirtualFile
import java.io.File
import java.io.PrintStream
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

abstract class SqlDelightProjectTestCase : LightJavaCodeInsightFixtureTestCase() {
  protected val tempRoot: VirtualFile
    get() = module.rootManager.contentRoots.single()
  override fun setUp() {
    super.setUp()
    DialectPreset.SQLITE_3_18.setup()
    SqldelightParserUtil.overrideSqlParser()
    myFixture.copyDirectoryToProject("", "")
    SqlDelightFileIndex.setInstance(module, FileIndex(configurePropertiesFile(), tempRoot))
    ApplicationManager.getApplication().runWriteAction {
      generateSqlDelightFiles()
    }
  }

  override fun tearDown() {
    super.tearDown()
    File(testDataPath, SqlDelightPropertiesFile.NAME).delete()
  }

  override fun getTestDataPath() = "testData/project"

  open fun configurePropertiesFile(): SqlDelightDatabaseProperties {
    return SqlDelightDatabaseProperties(
        className = "QueryWrapper",
        packageName = "com.example",
        compilationUnits = listOf(
            SqlDelightCompilationUnit("internalDebug", listOf(SqlDelightSourceFolder("src/main/sqldelight", false), SqlDelightSourceFolder("src/internal/sqldelight", false), SqlDelightSourceFolder("src/debug/sqldelight", false), SqlDelightSourceFolder("src/internalDebug/sqldelight", false))),
            SqlDelightCompilationUnit("internalRelease", listOf(SqlDelightSourceFolder("src/main/sqldelight", false), SqlDelightSourceFolder("src/internal/sqldelight", false), SqlDelightSourceFolder("src/release/sqldelight", false), SqlDelightSourceFolder("src/internalRelease/sqldelight", false))),
            SqlDelightCompilationUnit("productionDebug", listOf(SqlDelightSourceFolder("src/main/sqldelight", false), SqlDelightSourceFolder("src/production/sqldelight", false), SqlDelightSourceFolder("src/debug/sqldelight", false), SqlDelightSourceFolder("src/productionDebug/sqldelight", false))),
            SqlDelightCompilationUnit("productionRelease", listOf(SqlDelightSourceFolder("src/main/sqldelight", false), SqlDelightSourceFolder("src/production/sqldelight", false), SqlDelightSourceFolder("src/release/sqldelight", false), SqlDelightSourceFolder("src/productionRelease/sqldelight", false)))
        ),
        outputDirectory = "build",
        dependencies = emptyList(),
        dialectPreset = DialectPreset.SQLITE_3_18
    )
  }

  protected inline fun <reified T : PsiElement> searchForElement(text: String): Collection<T> {
    return PsiTreeUtil.collectElements(file) {
      it is LeafPsiElement && it.text == text
    }.mapNotNull { it.getNonStrictParentOfType<T>() }
  }

  private fun generateSqlDelightFiles() {
    val mainDir = module.rootManager.contentRoots.single().findFileByRelativePath("src/main")!!
    val virtualFileWriter = { filePath: String ->
      val vFile: VirtualFile by GeneratedVirtualFile(filePath, module)
      PrintStream(vFile.getOutputStream(this))
    }
    var fileToGenerateDb: SqlDelightQueriesFile? = null
    module.rootManager.fileIndex.iterateContentUnderDirectory(mainDir) { file ->
      if (file.fileType != SqlDelightFileType) return@iterateContentUnderDirectory true
      val sqlFile = (psiManager.findFile(file)!! as SqlDelightQueriesFile)
      sqlFile.viewProvider.contentsSynchronized()
      fileToGenerateDb = sqlFile
      return@iterateContentUnderDirectory true
    }
    SqlDelightCompiler.writeInterfaces(module, fileToGenerateDb!!, module.name, virtualFileWriter)
    SqlDelightCompiler.writeDatabaseInterface(module, fileToGenerateDb!!, module.name, virtualFileWriter)
  }
}
