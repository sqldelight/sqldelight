/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.cash.sqldelight.test.util

import app.cash.sqldelight.core.compiler.SqlDelightCompiler
import app.cash.sqldelight.core.lang.MigrationFile
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.dialects.sqlite_3_18.SqliteDialect
import com.alecstrong.sql.psi.core.SqlAnnotationHolder
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.io.File
import org.junit.rules.TemporaryFolder

private typealias CompilationMethod = (Module, SqlDelightDialect, SqlDelightQueriesFile, (String) -> Appendable) -> Unit

object FixtureCompiler {

  fun compileSql(
    sql: String,
    temporaryFolder: TemporaryFolder,
    overrideDialect: SqlDelightDialect = SqliteDialect(),
    compilationMethod: CompilationMethod = SqlDelightCompiler::writeInterfaces,
    fileName: String = "Test.sq",
    treatNullAsUnknownForEquality: Boolean = false,
    generateAsync: Boolean = false,
  ): CompilationResult {
    writeSql(sql, temporaryFolder, fileName)
    return compileFixture(
      temporaryFolder.fixtureRoot().path,
      compilationMethod,
      overrideDialect = overrideDialect,
      treatNullAsUnknownForEquality = treatNullAsUnknownForEquality,
      generateAsync = generateAsync,
    )
  }

  fun writeSql(
    sql: String,
    temporaryFolder: TemporaryFolder,
    fileName: String,
  ) {
    val srcRootDir = temporaryFolder.fixtureRoot().apply { mkdirs() }
    val fixtureSrcDir = File(srcRootDir, "com/example").apply { mkdirs() }
    File(fixtureSrcDir, fileName).apply {
      parentFile.mkdirs()
      createNewFile()
      writeText(sql)
    }
  }

  fun parseSql(
    sql: String,
    temporaryFolder: TemporaryFolder,
    fileName: String = "Test.sq",
    dialect: SqlDelightDialect = SqliteDialect(),
    treatNullAsUnknownForEquality: Boolean = false,
    generateAsync: Boolean = false,
  ): SqlDelightQueriesFile {
    writeSql(sql, temporaryFolder, fileName)
    val errors = mutableListOf<String>()
    val parser = TestEnvironment(dialect = dialect, treatNullAsUnknownForEquality = treatNullAsUnknownForEquality, generateAsync = generateAsync)
    val environment = parser.build(
      temporaryFolder.fixtureRoot().path,
      createAnnotationHolder(errors),
    )

    if (errors.isNotEmpty()) {
      throw AssertionError("Got unexpected errors\n\n$errors")
    }

    var file: SqlDelightQueriesFile? = null
    environment.forSourceFiles {
      if (it.name == fileName) file = it as SqlDelightQueriesFile
    }
    return file!!
  }

  fun compileFixture(
    fixtureRoot: String,
    compilationMethod: CompilationMethod = SqlDelightCompiler::writeInterfaces,
    overrideDialect: SqlDelightDialect = SqliteDialect(),
    generateDb: Boolean = true,
    writer: ((String) -> Appendable)? = null,
    outputDirectory: File = File(fixtureRoot, "output"),
    deriveSchemaFromMigrations: Boolean = false,
    treatNullAsUnknownForEquality: Boolean = false,
    generateAsync: Boolean = false,
  ): CompilationResult {
    val compilerOutput = mutableMapOf<File, StringBuilder>()
    val errors = mutableListOf<String>()
    val sourceFiles = StringBuilder()
    val parser = TestEnvironment(outputDirectory, deriveSchemaFromMigrations, treatNullAsUnknownForEquality, overrideDialect, generateAsync)
    val fixtureRootDir = File(fixtureRoot)
    require(fixtureRootDir.exists()) { "$fixtureRoot does not exist" }

    val environment = parser.build(fixtureRootDir.path, createAnnotationHolder(errors))
    val fileWriter = writer ?: fileWriter@{ fileName: String ->
      val builder = StringBuilder()
      compilerOutput += File(fileName) to builder
      return@fileWriter builder
    }

    var file: SqlDelightQueriesFile? = null
    var topMigration: MigrationFile? = null

    environment.forSourceFiles { psiFile ->
      psiFile.log(sourceFiles)
      if (psiFile is SqlDelightQueriesFile) {
        if (errors.isEmpty()) {
          compilationMethod(environment.module, environment.dialect, psiFile, fileWriter)
        }
        file = psiFile
      } else if (psiFile is MigrationFile) {
        if (topMigration == null || psiFile.order > topMigration!!.order) {
          topMigration = psiFile
        }
      }
    }

    if (topMigration != null) {
      SqlDelightCompiler.writeInterfaces(
        file = topMigration!!,
        output = fileWriter,
        includeAll = true,
      )
    }

    if (generateDb) {
      SqlDelightCompiler.writeDatabaseInterface(environment.module, file!!, "testmodule", fileWriter)
      SqlDelightCompiler.writeImplementations(
        environment.module,
        file!!,
        "testmodule",
        fileWriter,
      )
    }

    return CompilationResult(outputDirectory, compilerOutput, errors, sourceFiles.toString(), file!!)
  }

  private fun createAnnotationHolder(
    errors: MutableList<String>,
  ) = SqlAnnotationHolder { element, message ->
    val documentManager = PsiDocumentManager.getInstance(element.project)
    val name = element.containingFile.name
    val document = documentManager.getDocument(element.containingFile)!!
    val lineNum = document.getLineNumber(element.textOffset)
    val offsetInLine = element.textOffset - document.getLineStartOffset(lineNum)
    errors += "$name: (${lineNum + 1}, $offsetInLine): $message"
  }

  private fun PsiFile.log(sourceFiles: StringBuilder) {
    sourceFiles.append("$name:\n")
    printTree {
      sourceFiles.append("  ")
      sourceFiles.append(this)
    }
  }

  private fun PsiElement.printTree(printer: (String) -> Unit) {
    printer("$this\n")
    children.forEach { child ->
      child.printTree { printer("  $it") }
    }
  }

  data class CompilationResult(
    val outputDirectory: File,
    val compilerOutput: Map<File, StringBuilder>,
    val errors: List<String>,
    val sourceFiles: String,
    val compiledFile: SqlDelightQueriesFile,
  )
}

fun TemporaryFolder.fixtureRoot() = File(root, "src/test/test-fixture")
