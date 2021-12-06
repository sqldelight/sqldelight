package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.SqlDelightCompilationUnit
import app.cash.sqldelight.core.SqlDelightDatabaseName
import app.cash.sqldelight.core.SqlDelightDatabaseProperties
import app.cash.sqldelight.core.SqlDelightSourceFolder
import app.cash.sqldelight.core.SqldelightParserUtil
import app.cash.sqldelight.core.compiler.SqlDelightCompiler
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.intellij.gradle.FileIndexMap
import app.cash.sqldelight.intellij.util.GeneratedVirtualFile
import com.alecstrong.sql.psi.core.DialectPreset
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.io.File
import java.io.PrintStream

abstract class SqlDelightProjectTestCase : LightJavaCodeInsightFixtureTestCase() {
  protected val tempRoot: VirtualFile
    get() = module.rootManager.contentRoots.single()
  override fun setUp() {
    super.setUp()
    DialectPreset.SQLITE_3_18.setup()
    SqldelightParserUtil.overrideSqlParser()
    myFixture.copyDirectoryToProject("", "")
    FileIndexMap.defaultIndex = FileIndex(configurePropertiesFile(), tempRoot)
    ApplicationManager.getApplication().runWriteAction {
      generateSqlDelightFiles()
    }
  }

  override fun getTestDataPath() = "testData/project"

  open fun configurePropertiesFile(): SqlDelightDatabaseProperties {
    return SqlDelightDatabasePropertiesImpl(
      className = "QueryWrapper",
      packageName = "com.example",
      compilationUnits = listOf(
        SqlDelightCompilationUnitImpl(
          name = "internalDebug",
          outputDirectoryFile = File(tempRoot.path, "build"),
          sourceFolders = listOf(SqlDelightSourceFolderImpl(File(tempRoot.path, "src/main/sqldelight"), false), SqlDelightSourceFolderImpl(File(tempRoot.path, "src/internal/sqldelight"), false), SqlDelightSourceFolderImpl(File(tempRoot.path, "src/debug/sqldelight"), false), SqlDelightSourceFolderImpl(File(tempRoot.path, "src/internalDebug/sqldelight"), false))
        ),
        SqlDelightCompilationUnitImpl(
          name = "internalRelease",
          outputDirectoryFile = File(tempRoot.path, "build"),
          sourceFolders = listOf(SqlDelightSourceFolderImpl(File(tempRoot.path, "src/main/sqldelight"), false), SqlDelightSourceFolderImpl(File(tempRoot.path, "src/internal/sqldelight"), false), SqlDelightSourceFolderImpl(File(tempRoot.path, "src/release/sqldelight"), false), SqlDelightSourceFolderImpl(File(tempRoot.path, "src/internalRelease/sqldelight"), false))
        ),
        SqlDelightCompilationUnitImpl(
          name = "productionDebug",
          outputDirectoryFile = File(tempRoot.path, "build"),
          sourceFolders = listOf(SqlDelightSourceFolderImpl(File(tempRoot.path, "src/main/sqldelight"), false), SqlDelightSourceFolderImpl(File(tempRoot.path, "src/production/sqldelight"), false), SqlDelightSourceFolderImpl(File(tempRoot.path, "src/debug/sqldelight"), false), SqlDelightSourceFolderImpl(File(tempRoot.path, "src/productionDebug/sqldelight"), false))
        ),
        SqlDelightCompilationUnitImpl(
          name = "productionRelease",
          outputDirectoryFile = File(tempRoot.path, "build"),
          sourceFolders = listOf(SqlDelightSourceFolderImpl(File(tempRoot.path, "src/main/sqldelight"), false), SqlDelightSourceFolderImpl(File(tempRoot.path, "src/production/sqldelight"), false), SqlDelightSourceFolderImpl(File(tempRoot.path, "src/release/sqldelight"), false), SqlDelightSourceFolderImpl(File(tempRoot.path, "src/productionRelease/sqldelight"), false))
        )
      ),
      dependencies = emptyList(),
      dialectPresetName = DialectPreset.SQLITE_3_18.name,
      rootDirectory = File(tempRoot.path).absoluteFile,
      deriveSchemaFromMigrations = false
    )
  }

  private data class SqlDelightDatabasePropertiesImpl(
    override val packageName: String,
    override val compilationUnits: List<SqlDelightCompilationUnit>,
    override val className: String,
    override val dependencies: List<SqlDelightDatabaseName>,
    override val dialectPresetName: String,
    override val deriveSchemaFromMigrations: Boolean,
    override val rootDirectory: File
  ) : SqlDelightDatabaseProperties

  private data class SqlDelightSourceFolderImpl(
    override val folder: File,
    override val dependency: Boolean
  ) : SqlDelightSourceFolder

  private data class SqlDelightCompilationUnitImpl(
    override val name: String,
    override val sourceFolders: List<SqlDelightSourceFolder>,
    override val outputDirectoryFile: File,
  ) : SqlDelightCompilationUnit

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
    SqlDelightCompiler.writeInterfaces(module, fileToGenerateDb!!, virtualFileWriter)
    SqlDelightCompiler.writeDatabaseInterface(module, fileToGenerateDb!!, module.name, virtualFileWriter)
  }
}
