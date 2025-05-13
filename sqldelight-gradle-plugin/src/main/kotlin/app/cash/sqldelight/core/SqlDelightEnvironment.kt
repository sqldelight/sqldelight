/*
 * Copyright (C) 2017 Square, Inc.
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
package app.cash.sqldelight.core

import app.cash.sqldelight.core.annotators.OptimisticLockCompilerAnnotator
import app.cash.sqldelight.core.compiler.SqlDelightCompiler
import app.cash.sqldelight.core.lang.DatabaseFileType
import app.cash.sqldelight.core.lang.DatabaseFileViewProviderFactory
import app.cash.sqldelight.core.lang.MigrationFile
import app.cash.sqldelight.core.lang.MigrationFileType
import app.cash.sqldelight.core.lang.MigrationParserDefinition
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.core.lang.SqlDelightParserDefinition
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.core.lang.util.migrationFiles
import app.cash.sqldelight.core.psi.SqlDelightImportStmt
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import com.alecstrong.sql.psi.core.SqlCoreEnvironment
import com.alecstrong.sql.psi.core.SqlFileBase
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockModule
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleExtension
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileTypeFileViewProviders
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import java.io.File
import java.util.StringTokenizer
import kotlin.math.log10
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

/**
 * Mocks an intellij environment for compiling sqldelight files without an instance of intellij
 * running.
 */
class SqlDelightEnvironment(
  private val properties: SqlDelightDatabaseProperties,
  private val compilationUnit: SqlDelightCompilationUnit,
  private val verifyMigrations: Boolean,
  override var dialect: SqlDelightDialect,
  moduleName: String,
  private val sourceFolders: List<File> = compilationUnit.sourceFolders
    .filter { it.folder.exists() && !it.dependency }
    .map { it.folder },
  private val dependencyFolders: List<File> = compilationUnit.sourceFolders
    .filter { it.folder.exists() && it.dependency }
    .map { it.folder },
) : SqlCoreEnvironment(sourceFolders, dependencyFolders),
  SqlDelightProjectService {
  val project = projectEnvironment.project
  val module = MockModule(project, projectEnvironment.parentDisposable)
  private val moduleName = SqlDelightFileIndex.sanitizeDirectoryName(moduleName)

  init {
    project.registerService(SqlDelightProjectService::class.java, this)

    @Suppress("UnresolvedPluginConfigReference")
    CoreApplicationEnvironment.registerExtensionPoint(
      module.extensionArea,
      ExtensionPointName.create("com.intellij.moduleExtension"),
      ModuleExtension::class.java,
    )

    initializeApplication {
      registerFileType(MigrationFileType, MigrationFileType.defaultExtension)
      registerParserDefinition(MigrationParserDefinition())
      registerFileType(SqlDelightFileType, SqlDelightFileType.defaultExtension)
      registerParserDefinition(SqlDelightParserDefinition())
      registerFileType(DatabaseFileType, DatabaseFileType.defaultExtension)
      FileTypeFileViewProviders.INSTANCE.addExplicitExtension(DatabaseFileType, DatabaseFileViewProviderFactory())
    }
  }

  override var treatNullAsUnknownForEquality: Boolean = properties.treatNullAsUnknownForEquality

  override var generateAsync: Boolean = properties.generateAsync

  override fun module(vFile: VirtualFile) = module

  override fun fileIndex(module: Module): SqlDelightFileIndex = FileIndex()

  override fun resetIndex() = throw UnsupportedOperationException()

  override fun clearIndex() = throw UnsupportedOperationException()

  @JvmName("forSqlFileBases")
  fun forSourceFiles(action: (SqlFileBase) -> Unit) {
    forSourceFiles<SqlFileBase>(action)
  }

  override fun <T : PsiFile> forSourceFiles(
    klass: KClass<T>,
    action: (T) -> Unit,
  ) {
    super.forSourceFiles(klass) {
      if (it.fileType != MigrationFileType ||
        verifyMigrations ||
        properties.deriveSchemaFromMigrations
      ) {
        action(it)
      }
    }
  }

  /**
   * Run the SQLDelight compiler and return the error or success status.
   */
  fun generateSqlDelightFiles(logger: (String) -> Unit): CompilationStatus {
    val errors = sortedMapOf<Long, MutableList<String>>()
    val extraAnnotators = listOf(OptimisticLockCompilerAnnotator())
    annotate(
      extraAnnotators,
    ) { element, message ->
      val key = (element.containingFile as SqlDelightFile).order ?: Long.MAX_VALUE
      errors.putIfAbsent(key, ArrayList())
      errors[key]!!.add(errorMessage(element, message))
    }
    if (errors.isNotEmpty()) return CompilationStatus.Failure(errors.values.flatten())

    val writer = writer@{ fileName: String ->
      val file = File(fileName)
      if (!file.exists()) {
        file.parentFile.mkdirs()
        file.createNewFile()
      }
      return@writer file.writer()
    }

    var sourceFile: SqlDelightFile? = null
    var topMigrationFile: MigrationFile? = null
    forSourceFiles {
      if (it is MigrationFile && properties.deriveSchemaFromMigrations) {
        if (topMigrationFile == null || it.order > topMigrationFile!!.order) topMigrationFile = it
        if (sourceFile == null) sourceFile = it
      }

      if (it !is SqlDelightQueriesFile) return@forSourceFiles
      logger("----- START ${it.name} ms -------")
      val timeTaken = measureTimeMillis {
        SqlDelightCompiler.writeInterfaces(module, dialect, it, writer)
        sourceFile = it
      }
      logger("----- END ${it.name} in $timeTaken ms ------")
    }

    topMigrationFile?.let { migrationFile ->
      logger("----- START ${migrationFile.name} ms -------")
      val timeTaken = measureTimeMillis {
        SqlDelightCompiler.writeInterfaces(
          file = migrationFile,
          output = writer,
          includeAll = true,
        )
        SqlDelightCompiler.writeImplementations(module, migrationFile, moduleName, writer)
      }
      logger("----- END ${migrationFile.name} in $timeTaken ms ------")
    }

    sourceFile?.let {
      SqlDelightCompiler.writeDatabaseInterface(module, it, moduleName, writer)
      if (it is SqlDelightQueriesFile) {
        SqlDelightCompiler.writeImplementations(module, it, moduleName, writer)
      }
    }

    return CompilationStatus.Success
  }

  fun forMigrationFiles(body: (MigrationFile) -> Unit) {
    val psiManager = PsiManager.getInstance(projectEnvironment.project)
    val migrationFiles: Collection<MigrationFile> = sourceFolders
      .map { localFileSystem.findFileByPath(it.absolutePath)!! }
      .map { psiManager.findDirectory(it)!! }
      .flatMap { directory: PsiDirectory -> directory.migrationFiles() }
    migrationFiles.sortedBy { it.version }
      .forEach {
        val errorElements = ArrayList<PsiErrorElement>()
        PsiTreeUtil.processElements(it) { element ->
          when (element) {
            is PsiErrorElement -> errorElements.add(element)
            // Uncomment when sqm files understand their state of the world.
            // is SqlAnnotatedElement -> element.annotate(annotationHolder)
          }
          return@processElements true
        }
        if (errorElements.isNotEmpty()) {
          throw SqlDelightException(
            "Error Reading ${it.name}:\n\n" +
              errorElements.joinToString(separator = "\n") { errorMessage(it, it.errorDescription) },
          )
        }
        body(it)
      }
  }

  private fun errorMessage(element: PsiElement, message: String): String =
    "${element.containingFile.virtualFile.path}:${element.lineStart}:${element.charPositionInLine} $message\n${detailText(element)}"

  private fun detailText(element: PsiElement) = try {
    val context = context(element) ?: element
    val result = StringBuilder()
    val tokenizer = StringTokenizer(context.text, "\n", false)

    val maxDigits = (log10(context.lineEnd.toDouble()) + 1).toInt()
    for (line in context.lineStart..context.lineEnd) {
      if (!tokenizer.hasMoreTokens()) break
      val lineValue = tokenizer.nextToken()
      result.append(("%0${maxDigits}d    %s\n").format(line, lineValue))
      if (element.lineStart == element.lineEnd && element.lineStart == line) {
        // If it's an error on a single line highlight where on the line.
        result.append(("%${maxDigits}s    ").format(""))
        // Print tabs when you see it, spaces for everything else.
        lineValue.subSequence(0 until element.charPositionInLine)
          .map { char -> if (char != '\t') " " else char }
          .forEach(result::append)
        result.append(("%s\n").format("^".repeat(element.textLength)))
      }
    }

    result.toString()
  } catch (e: Exception) {
    // If there is an exception while trying to print an error, just give back the unformatted error
    // and print the stack trace for more debugging.
    e.printStackTrace()
    element.text
  }

  private val PsiElement.charPositionInLine: Int
    get() {
      val file = PsiDocumentManager.getInstance(project).getDocument(containingFile)!!
      return textOffset - file.getLineStartOffset(file.getLineNumber(textOffset))
    }

  private val PsiElement.lineStart: Int
    get() {
      val file = PsiDocumentManager.getInstance(project).getDocument(containingFile)!!
      return file.getLineNumber(textOffset) + 1
    }

  private val PsiElement.lineEnd: Int
    get() {
      val file = PsiDocumentManager.getInstance(project).getDocument(containingFile)!!
      return file.getLineNumber(textOffset + textLength) + 1
    }

  private fun context(element: PsiElement?): PsiElement? =
    when (element) {
      null -> element
      is SqlCreateTableStmt -> element
      is SqlStmt -> element
      is SqlDelightImportStmt -> element
      else -> context(element.parent)
    }

  sealed class CompilationStatus {
    object Success : CompilationStatus()
    class Failure(val errors: List<String>) : CompilationStatus()
  }

  private inner class FileIndex : SqlDelightFileIndex {
    override val contentRoot
      get() = throw UnsupportedOperationException("Content root only usable from IDE")

    override val packageName = properties.packageName
    override val className = properties.className
    override val dependencies = properties.dependencies
    override val isConfigured = true
    override val deriveSchemaFromMigrations = properties.deriveSchemaFromMigrations

    override fun outputDirectory(file: SqlDelightFile) = outputDirectories()
    override fun outputDirectories(): List<String> {
      return listOf(compilationUnit.outputDirectoryFile.absolutePath)
    }

    private val virtualDirectoriesWithDependencies: List<VirtualFile> by lazy {
      return@lazy (sourceFolders + dependencyFolders)
        .map { localFileSystem.findFileByPath(it.absolutePath)!! }
    }

    private val directoriesWithDependencies: List<PsiDirectory> by lazy {
      val psiManager = PsiManager.getInstance(projectEnvironment.project)
      return@lazy virtualDirectoriesWithDependencies.map { psiManager.findDirectory(it)!! }
    }

    private val virtualDirectories: List<VirtualFile> by lazy {
      return@lazy sourceFolders
        .map { localFileSystem.findFileByPath(it.absolutePath)!! }
    }

    private val directories: List<PsiDirectory> by lazy {
      val psiManager = PsiManager.getInstance(projectEnvironment.project)
      return@lazy virtualDirectories.map { psiManager.findDirectory(it)!! }
    }

    override fun packageName(file: SqlDelightFile): String {
      fun PsiFileSystemItem.relativePathUnder(ancestor: PsiDirectory): List<String>? {
        if (this.virtualFile.path == ancestor.virtualFile.path) return emptyList()
        parent?.let {
          return it.relativePathUnder(ancestor)?.plus(name)
        }
        return null
      }

      for (sourceFolder in sourceFolders(file)) {
        val path = file.parent!!.relativePathUnder(sourceFolder)
        if (path != null) {
          return path.joinToString(separator = ".") {
            SqlDelightFileIndex.sanitizeDirectoryName(
              it,
            )
          }
        }
      }

      throw IllegalStateException(
        "Tried to find package name of file ${file.virtualFile!!.path} when" +
          " it is not under any of the source folders $sourceFolders",
      )
    }

    override fun sourceFolders(file: VirtualFile, includeDependencies: Boolean) =
      if (includeDependencies) virtualDirectoriesWithDependencies else virtualDirectories

    override fun sourceFolders(file: SqlDelightFile, includeDependencies: Boolean) =
      if (includeDependencies) directoriesWithDependencies else directories
  }
}
